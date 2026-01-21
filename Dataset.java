import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
* Class used to read ARFF files.
*
* @author Fernando Otero
* @version 1.0
*/
public class Dataset {
    /**
    * Constant representing an attribute section.
    */
    private static final String ATTRIBUTE = "@attribute";
    
    /**
    * Constant representing the data section.
    */
    private static final String DATA = "@data";
    
    /**
    * Constant representing the relation section.
    */
    private static final String RELATION = "@relation";
    
    /**
    * The list of attributes.
    */
    private ArrayList<Attribute> attributes;
    
    /**
    * Returns the value of the target attribute.
    * 
    * @param encoding the encoded rule/instance.
    * 
    * @returns the value of the target attribute.
    */
    public boolean target(boolean[] encoding) {
        return encoding[encoding.length - 1];
    }
    
    /**
    * Returns <code>true</code> if the rule covers the specified instance.
    * 
    * @param rule the rule array.
    * @param instance the instance array.
    * 
    * @return <code>true</code> if the rule covers the instance; <code>false</code>
    *         otherwise.
    */
    public boolean covers(boolean[] rule, boolean[] instance) {
        int position = 0;
        
        for (int i = 0; i < attributes.size() - 1; i++) {
            //Integer variables
            int instanceValue = 0;

            attributes.get(i).isNumeric = isNumeric(i);
            
            int length = attributes.get(i).length();
            int ifNumericLength = 0;
            boolean relationalBit = false;

            if(attributes.get(i).isNumeric) {
                ifNumericLength = length - 1;
                relationalBit = rule[length - 1];
            } else
                ifNumericLength = length;
            
            boolean match = false;
            int count = 0;
            
            for (int j = 0; j < ifNumericLength; j++) {
                if (rule[position + j]) {
                    count++;
                    
                    if (instance[position + j]) {
                        match = true;
                        instanceValue++;
                    }
                }
            }
            
            if(attributes.get(i).isNumeric) {
                if(relationalBit) {
                    if(instanceValue > count) return false;
                } else {
                    if(instanceValue < count) return false;
                }
            }
            // else {
                if (count != (attributes.get(i).isNumeric ? length - 1 : length) && !match) {
                    return false;
                }
            // }
            
            position += length;
        }
        return true;
    }
    
    private Boolean isNumeric(int number) {
        if (number == 1 || number == 2 || number == 6 || number == 9 || number == 12 || number == 13) {
            attributes.get(number).isNumeric = true;
            return true;
        }
        attributes.get(number).isNumeric = false;
        return false;
    }
    
    public String toString(boolean[] rule) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("IF ");
        
        int position = 0;
        boolean first = true;
        
        for (int i = 0; i < attributes.size() - 1; i++) {
            int length = attributes.get(i).length();
            int isNumericLength = attributes.get(i).isNumeric ? length - 1 : length;
            int count = 0;
            
            StringBuffer test = new StringBuffer();
            test.append("(");
            test.append(attributes.get(i).name);
            
            if(attributes.get(i).isNumeric) {
                if(rule[length-1])
                    test.append(" > ");
                else
                    test.append(" < ");
            } else {
                test.append(" = ");
            }
            
            for (int j = 0; j < isNumericLength; j++) {
                if (rule[position + j]) {
                    if (count > 0) {
                        test.append(" OR ");
                    }
                    test.append(attributes.get(i).values.get(j));
                    count++;
                }
            }
            
            test.append(")");
            
            if (count > 0 && count < isNumericLength) {
                if (!first) {
                    buffer.append(" AND ");
                } else {
                    first = false;
                }
                
                buffer.append(test);
            }
            
            position += length;
        }
        
        if (first) {
            buffer.append("<empty>");
        }
        
        buffer.append(" THEN ");
        
        Attribute target = attributes.get(attributes.size() - 1);
        buffer.append(target.value(rule[rule.length - 1]));
        
        return buffer.toString();
    }
    
    /**
    * Reads the specified input reader. The reader will be closed at the end of
    * the method.
    * 
    * @param input
    *            a reader.
    * 
    * @return a <code>Dataset</code> instance contaning the contents of the
    *         input reader.
    * 
    * @exception IOException
    *                if an I/O error occurs.
    */
    public ArrayList<boolean[]> read(String input) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line = null;
        boolean dataSection = false;
        
        ArrayList<boolean[]> instances = new ArrayList<>();
        attributes = new ArrayList<>();
        int length = 0;
        
        while ((line = reader.readLine()) != null) {
            String[] split = split(line);
            
            if (split.length > 0 && !isComment(split[0])) {
                split[0] = split[0].toLowerCase();
                
                // are we dealing with an attribute?
                if (split[0].startsWith(ATTRIBUTE)) {
                    if (split.length != 3) {
                        reader.close();
                        throw new IllegalArgumentException("Invalid attribute specification: "
                        + line);
                    } else {
                        processAttribute(split);
                    }
                } else if (split[0].startsWith(DATA)) {
                    dataSection = true;
                    
                    for (Attribute attribute : attributes) {
                        length += attribute.length();
                    }
                    
                    Attribute target = attributes.get(attributes.size() - 1);
                    
                    if (target.length() != 2) {
                        throw new IllegalArgumentException("Unsupported class attribute: "
                        + target.length() + " values found");
                    }
                    
                    length -= 1;
                }
                // we must be dealing with an instance
                else if (dataSection) {
                    instances.add(processInstance(line, length));
                }
            }
        }
        
        reader.close();
        
        return instances;
    }
    
    /**
    * Parses an attribute.
    * 
    * //@param dataset
    *            the dataset being read.
    * @param components
    *            the components representing the attribute.
    */
    private void processAttribute(String[] components) {
        if (components[2].startsWith("{")) {
            // it is a nominal attribute
            Attribute attribute = new Attribute();
            attribute.name = components[1];
            StringBuffer value = new StringBuffer();
            
            for (int i = 0; i < components[2].length(); i++) {
                if (components[2].charAt(i) == ',') {
                    attribute.values.add(trim(value.toString()));
                    value.delete(0, value.length());
                } else if (components[2].charAt(i) == '}') {
                    attribute.values.add(trim(value.toString()));
                    attributes.add(attribute);
                    break;
                } else if (components[2].charAt(i) != '{') {
                    value.append(components[2].charAt(i));
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported attribute: "
            + components[1]);
        }
    }
    
    /**
    * Parses an instance and adds it to the current dataset.
    * 
    * //@param dataset
    *            the dataset being read.
    * @param line
    *            the instance information.
    */
    private boolean[] processInstance(String line, int length) {
        StringTokenizer tokens = new StringTokenizer(line, ",");
        boolean[] instance = new boolean[length];
        
        int index = 0;
        int position = 0;
        
        while (tokens.hasMoreTokens()) {
            String value = trim(tokens.nextToken());
            
            Attribute attribute = attributes.get(index);
            
            if (index == attributes.size() - 1) {
                instance[position] = attribute.target(value);
            } else {
                boolean[] encoding = attribute.toBinary(value);
                
                for (int i = 0; i < encoding.length; i++) {
                    instance[position + i] = encoding[i];
                }
                
                position += encoding.length;
            }
            
            index++;
        }
        
        return instance;
    }
    
    /**
    * Checks if the line is a comment.
    * 
    * @param line
    *            the line to be checked.
    * 
    * @return <code>true</code> if the line is a comment; <code>false</code>
    *         otherwise.
    */
    private boolean isComment(String line) {
        if (line.startsWith("%") || line.startsWith("#")) {
            return true;
        }
        
        return false;
    }
    
    /**
    * Removes spaces from the beginning and end of the string. This method will
    * also remove single quotes usually created by WEKA discretisation process.
    * 
    * @param value
    *            the string to trim.
    * 
    * @return a string without spaces at the beginning and end.
    */
    private String trim(String value) {
        value = value.replace("'\\'", "\"").replace("\\''", "\"");
        return value.trim();
    }
    
    /**
    * Divides the input String into tokens, using a white space as delimiter.
    * 
    * @param line
    *            the String to be divided.
    * 
    * @return an array of String representing the tokens.
    */
    private String[] split(String line) {
        String[] words = new String[0];
        int index = 0;
        
        while (index < line.length()) {
            StringBuffer word = new StringBuffer();
            
            boolean copying = false;
            boolean quotes = false;
            boolean brackets = false;
            
            int i = index;
            
            for (; i < line.length(); i++) {
                char c = line.charAt(i);
                
                if (!copying && !Character.isWhitespace(c)) {
                    copying = true;
                }
                
                if (c == '"' || c == '\'') {
                    quotes ^= true;
                } else if (c == '{' || c == '}') {
                    brackets ^= true;
                }
                
                if (copying) {
                    if (Character.isWhitespace(c) && !quotes && !brackets) {
                        index = i + 1;
                        break;
                    }
                    
                    word.append(c);
                }
            }
            
            if (i >= line.length()) {
                // we reached the end of the line, need to stop the while loop
                index = i;
            }
            
            if (word.length() > 0) {
                words = Arrays.copyOf(words, words.length + 1);
                words[words.length - 1] = word.toString();
            }
        }
        
        return words;
    }
    
    /**
    * Class that represents an attribute.
    */
    public static class Attribute {
        /**
        * The name of the attribute.
        */
        String name;
        
        /**
        * The values of the attribute.
        */
        ArrayList<String> values = new ArrayList<>();
        
        Boolean isNumeric = false;
        
        int length() {
            return values.size();
        }
        
        /**
        * Returns the binary representation for the specified value.
        * 
        * @param value the attribute value.
        */
        boolean[] toBinary(String value) {
            boolean[] encoded = new boolean[length()];
            
            for (int i = 0; i < encoded.length; i++) {
                encoded[i] = value.equals(values.get(i));
            }
            
            return encoded;
        }
        
        /**
        * Returns the boolean value for the specified target value.
        * 
        * @param value the target value.
        * 
        * @return the boolean value for the specified target value.
        */
        boolean target(String value) {
            int index = values.indexOf(value);
            return index != 0;
        }
        
        String value(boolean value) {
            return value ? values.get(1) : values.get(0);
        }
    }
}