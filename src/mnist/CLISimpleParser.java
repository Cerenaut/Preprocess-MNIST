/* Copyright (c) 2017.
        *
        * This file is part of Project AGI. <http://agi.io>
        *
        * Project AGI is distributed in the hope that it will be useful,
        * but WITHOUT ANY WARRANTY; without even the implied warranty of
        * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        * GNU General Public License for more details.
        *
        * You should have received a copy of the GNU General Public License
        * along with Project AGI.  If not, see <http://www.gnu.org/licenses/>.
        */


package mnist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by:  Richard Masoumi
 * Date:        30/3/17
 */

public class CLISimpleParser {

    private Map<String, String> parsedArguments;
    private List<ArgumentEntry> argumentEntries;
    private boolean alreadyParsed = false;

    public CLISimpleParser(List<ArgumentEntry> argumentEntries){

        parsedArguments = new HashMap<>();
        this.argumentEntries = argumentEntries;

    }


    public Map<String, String> parse(String[] programArguments) throws Exception{

        //being lazy and just return a cached calculated value
        if(alreadyParsed){
            return parsedArguments;
        }

        StringBuilder stringBuilder = new StringBuilder();

        //turning input arguments into a simple String for processing with Regex

        for(String programArgument : programArguments){
            stringBuilder.append(programArgument).append(" ");
        }

        String rawArguments = stringBuilder.toString();

        Pattern argumentsPattern = Pattern.compile("\\B--?.*?(?=\\B-|$)");
        Matcher matcher = argumentsPattern.matcher(rawArguments);

        while(matcher.find()) {

            String current_argument = matcher.group().trim();
            String[] current_tokens = current_argument.split("\\s");


            //if we have encounter a short argument format, it must be exactly 2 char long, the first char must be
            //"-" and the last char must be a letter
            if(current_tokens[0].length() == 2){
                if(!(current_tokens[0].charAt(0) == '-'   && Character.isLetter(current_tokens[0].charAt(1))) ) {
                    throw new Exception("Invalid input argument: " + current_tokens[0] + System.lineSeparator());
                }

            }
            //we reject all inputs which starts with 2 "-" and a single char (eg. --r)
            else if (current_tokens[0].length() == 3) {
                throw new Exception("Invalid input argument: " + current_tokens[0] + System.lineSeparator());

            }
            //we reject inputs like -randomise. The correct format is --randomise
            else if (current_tokens[0].length() > 3) {
                if (!(current_tokens[0].charAt(0) == '-' && current_tokens[0].charAt(1) == '-')) {
                    throw new Exception("Invalid input argument: " + current_tokens[0] + System.lineSeparator());
                }
            }

            String argument = current_tokens[0].replace("-", "");

            boolean unknownArgument = true;
            //looping through the argument list to find the current parsed argument
            for (ArgumentEntry argumentEntry : argumentEntries) {

                if(     argumentEntry.getLongName().equals(argument) ||
                        argumentEntry.getShortName().equals(argument)) {

                    //making sure that a flag argument has no parameter
                    if (current_tokens.length == 1) {

                        if (!argumentEntry.isFlag()) {

                            throw new Exception("Invalid input argument. " + argumentEntry.getLongName() + " is a flag argument with no parameter," +
                                    "but you have provided a parameter for it.");
                        }

                        //we already have seen the current argument before, so it has been provided twice by the user. Error condition
                        if(!parsedArguments.containsKey(argumentEntry.getLongName())) {

                            //the presence of a flag argument always interpreted as true boolean value
                            parsedArguments.put(argumentEntry.getLongName(), "true");
                            unknownArgument = false;

                        } else {

                            throw new Exception("Invalid input argument. You have provided a flag argument " + argumentEntry.getLongName() +
                            " twice. Every argument should only be provided once.");
                        }

                        //we have found what we are looking for... skipping the rest of the arguments array
                        break;

                    } else {

                        //TODO: even though current_tokens holds all the parameters provided for an argument, at the moment we just put the first argument in the map. It has to be fixed in later versions.

                        //we already have seen the current argument before, so it has been provided twice by the user. Error condition
                        if(!parsedArguments.containsKey(argumentEntry.getLongName())) {

                            parsedArguments.put(argumentEntry.getLongName(), current_tokens[1]);

                            unknownArgument = false;

                        } else {

                            throw new Exception("Invalid input argument. You have provided an argument " + argumentEntry.getLongName() +
                                    " twice. Every argument should only be provided once.");
                        }

                        //we have found what we are looking for... skipping the rest of the arguments array
                        break;
                    }

                }
            } //end of looping through the list ArgumentEntries

            //the current parsed input argument is undefined.
            if(unknownArgument) {
                //String prefixedArgName = (current_tokens[0].length() == 1) ? "-" : "--";
                //prefixedArgName += current_tokens[0];

                throw new Exception("Unrecognised input parameter: " + current_tokens[0]);
            }

        }

        //filling the parsedArguments map with the default values of optional arguments
        for(ArgumentEntry argumentEntry : argumentEntries){

            if(argumentEntry.isOptional() && !parsedArguments.containsKey(argumentEntry.getLongName())){
                parsedArguments.put(argumentEntry.getLongName(), argumentEntry.getDefaultValue());
            }
        }


        //making sure we are not missing any arguments
        List<ArgumentEntry> missingArguments = new ArrayList<>();
        for(ArgumentEntry argumentEntry : argumentEntries){

            if(!parsedArguments.containsKey(argumentEntry.getLongName())) {
                missingArguments.add(argumentEntry);
            }

        }

        if(missingArguments.size() != 0) {
            StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append("You are missing the following expected input arguments: " + System.lineSeparator() + System.lineSeparator());

            for(ArgumentEntry missingEntry : missingArguments){
                errorBuilder.append(missingEntry.getLongName()).append(":" + System.lineSeparator()).append(missingEntry.getDescription() + System.lineSeparator());
            }

            throw new Exception(errorBuilder.toString());
        }


        alreadyParsed = true;
        return parsedArguments;
    }

    public static class ArgumentEntry {
        private String            shortName;
        private String            longName;
        private String            defaultValue;
        private String            description;
        private boolean           _isFlag;
        private boolean           _isOptional;
        private Predicate<String> validationPredicate;

        public String getShortName() {
            return shortName;
        }

        public boolean isFlag() {
            return _isFlag;
        }

        public String getDefaultValue(){
            return defaultValue;
        }

        public boolean isOptional(){
            return _isOptional;
        }

        public String getDescription(){
            return description;
        }

        public String getLongName() {
            return longName;
        }

        public boolean validate(String args) throws Exception{

            return validationPredicate.test(args);
        }

        public ArgumentEntry(String shortName, String longName, String description, boolean isOptional, boolean isFlag, String defaultValue, Predicate<String> validationPredicate){
            this.shortName           = shortName;
            this.longName            = longName;
            this._isFlag             = isFlag;
            this.defaultValue        = defaultValue;
            this._isOptional         = isOptional;
            this.description         = description;
            this.validationPredicate = validationPredicate;
        }

    }
}