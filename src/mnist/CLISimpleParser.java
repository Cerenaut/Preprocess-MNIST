/*
 * Copyright (c) 2017.
 *
 * This file is part of Project AGI. <http://agi.io>
 *
 * Project AGI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import java.util.stream.Collectors;

/**
 * Created by:  Richard Masoumi
 * Date:        30/3/17
 */

/**
 * Provides basic parsing and validation functionality for CLI (command-line interface) input arguments.
 * It is capable of parsing/validating input arguments in their short-form (e.g. -r), or long-form (e.g. --randomise)
 * and return the parsed values in a Map&lt;String, String&gt;.
 * <p>
 * <p>
 * In order to use this class, it should be initialised by a list of {@link ArgumentEntry}
 * representing the expected input arguments. After initialisation, the method {@link CLISimpleParser#parse(String[] input_args)}
 * can be used to parse the given input string into a Map of parsed arguments. Input parameter <code>input_args</code>
 * is usually the exact variable given into the Main method at startup.
 * </p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * Both argument name and its value are validated. The argument's value validation is delegated to
 * {@link ArgumentEntry} class,
 * but the argument's name is validated based on the following rules:
 * <p>
 * </p>
 * <p>
 * <ul>
 * <li>
 * Short-form arguments must start with a single "-" immediately followed by only one character.
 * <p>
 * <p style="margin-left: 20px; font-style:italic">
 * example:
 * <br>
 * <code>
 * valid short-form arguments: -r  -p   -x
 * </code>
 * <p>
 * <br>
 * <p>
 * <code>
 * invalid short-form arguments: -xx -% --t
 * </code>
 * </p>
 * <p>
 * </li>
 * <li>
 * Long-form arguments must start with double "-" immediately followed by more that one character.
 * <br>
 * <p style="margin-left: 20px; font-style:italic">
 * example:
 * <br>
 * <code>
 * valid long-form arguments: --randomise  --number   -x
 * </code>
 * <p>
 * <br>
 * <p>
 * <code>
 * invalid short-form arguments: -randomise --r --#@
 * </code>
 * </p>
 * </li>
 * </ul>
 */

public class CLISimpleParser {

    private Map< String, String > parsedArguments;
    private List< ArgumentEntry > argumentEntries;
    private boolean alreadyParsed = false;

    /**
     * Initialises the class with a list of {@link ArgumentEntry} as registered input arguments
     *
     * @param argumentEntries {@link ArgumentEntry} to be registered
     */
    public CLISimpleParser( List< ArgumentEntry > argumentEntries ) {

        parsedArguments = new HashMap<>();
        this.argumentEntries = argumentEntries;

    }

    /**
     * Transforms the input argument array (same as given to the Main method at the startup) into a Map&lt;String, String&gt;
     * of key/values
     *
     * @param programArguments Input String array to be parsed
     * @return parsed argument/value
     * @throws Exception in case the input cannot be parsed, or the parsed argument does not match registered {@link ArgumentEntry} list.
     */
    public Map< String, String > parse( String[] programArguments ) throws Exception {

        // TODO: throwing specific exceptions based on a particular error condition instead of general Exception


        // returning a cached calculated value
        if( alreadyParsed ) {
            return parsedArguments;
        }

        StringBuilder stringBuilder = new StringBuilder();

        // turning input arguments into a simple String for processing with Regex
        for( String programArgument : programArguments ) {
            stringBuilder.append( programArgument ).append( " " );
        }

        String rawArguments = stringBuilder.toString();

        Pattern argumentsPattern = Pattern.compile( "\\B--?.*?(?=\\B-|$)" );
        Matcher matcher = argumentsPattern.matcher( rawArguments );

        while( matcher.find() ) {

            String current_argument = matcher.group().trim();
            String[] current_tokens = current_argument.split( "\\s" );


            // if we have encounter a short argument format, it must be exactly 2 char long, the first char must be
            // "-" and the last char must be a letter
            if( current_tokens[ 0 ].length() == 2 ) {
                if( !( current_tokens[ 0 ].charAt( 0 ) == '-' && Character.isLetter( current_tokens[ 0 ].charAt( 1 ) ) ) ) {
                    throw new Exception( "Invalid input argument: " + current_tokens[ 0 ] + System.lineSeparator() );
                }

            }

            // we reject all inputs which starts with 2 "-" and a single char (eg. --r)
            else if( current_tokens[ 0 ].length() == 3 ) {
                throw new Exception( "Invalid input argument: " + current_tokens[ 0 ] + System.lineSeparator() );

            }

            // we reject inputs like -randomise. The correct format is --randomise
            else if( current_tokens[ 0 ].length() > 3 ) {
                if( !( current_tokens[ 0 ].charAt( 0 ) == '-' && current_tokens[ 0 ].charAt( 1 ) == '-' ) ) {
                    throw new Exception( "Invalid input argument: " + current_tokens[ 0 ] + System.lineSeparator() );
                }
            }

            String argument = current_tokens[ 0 ].replace( "-", "" );

            // a flag to indicate there was an unknown argument on the input argument list
            boolean unknownArgument = true;

            // looping through the argument list to find the current parsed argument
            for( ArgumentEntry argumentEntry : argumentEntries ) {

                if( argumentEntry.getLongName().equals( argument ) ||
                        argumentEntry.getShortName().equals( argument ) ) {

                    // making sure that a flag argument has no parameter
                    if( current_tokens.length == 1 ) {

                        if( !argumentEntry.isFlag() ) {

                            throw new Exception( "Invalid input argument. " + argumentEntry.getLongName() + " is a flag argument with no parameter," +
                                    "but you have provided a parameter for it." );
                        }

                        // we already have seen the current argument before, so it has been provided twice by the user. Error condition
                        if( !parsedArguments.containsKey( argumentEntry.getLongName() ) ) {

                            // the presence of a flag argument always interpreted as true boolean value
                            parsedArguments.put( argumentEntry.getLongName(), "true" );
                            unknownArgument = false;

                        } else {

                            throw new Exception( "Invalid input argument. You have provided a flag argument " + argumentEntry.getLongName() +
                                    " twice. Every argument should only be provided once." );
                        }

                        // we have found what we are looking for... skipping the rest of the arguments array
                        break;

                    } else {

                        // TODO: even though current_tokens holds all the parameters provided for an argument, at the moment we just put the first argument in the map. It has to be fixed in later versions.

                        // we already have seen the current argument before, so it has been provided twice by the user. Error condition
                        if( !parsedArguments.containsKey( argumentEntry.getLongName() ) ) {

                            parsedArguments.put( argumentEntry.getLongName(), current_tokens[ 1 ] );

                            unknownArgument = false;

                        } else {

                            throw new Exception( "Invalid input argument. You have provided an argument " + argumentEntry.getLongName() +
                                    " twice. Every argument should only be provided once." );
                        }

                        // we have found what we are looking for... skipping the rest of the arguments array
                        break;
                    }

                }
            } // end of looping through the list ArgumentEntries

            // the current parsed input argument is undefined.
            if( unknownArgument ) {
                throw new Exception( "Unrecognised input parameter: " + current_tokens[ 0 ] );
            }

        }

        // filling the parsedArguments map with the default values of optional arguments
        for( ArgumentEntry argumentEntry : argumentEntries ) {

            if( argumentEntry.isOptional() && !parsedArguments.containsKey( argumentEntry.getLongName() ) ) {
                parsedArguments.put( argumentEntry.getLongName(), argumentEntry.getDefaultValue() );
            }
        }


        // making sure we are not missing any arguments
        List< ArgumentEntry > missingArguments = new ArrayList<>();
        for( ArgumentEntry argumentEntry : argumentEntries ) {

            if( !parsedArguments.containsKey( argumentEntry.getLongName() ) ) {
                missingArguments.add( argumentEntry );
            }

        }

        // if a help-typed argument is present within the parsed arguments, we shouldn't complain about any missing
        // arguments since we throw away every argument and just show the usage message.
        List<ArgumentEntry> helpArguments = argumentEntries.stream().filter( argument -> argument.isHelp ).collect( Collectors.toList());
        boolean isParsedHelpArgumentPresent = false;

        for(ArgumentEntry helpArgument : helpArguments){
            if (parsedArguments.containsKey( helpArgument.getLongName() )) {
                isParsedHelpArgumentPresent = true;
                break;
            }
        }

        if( (missingArguments.size() != 0) && (!isParsedHelpArgumentPresent) ) {
            StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append( "You are missing the following expected input arguments: " + System.lineSeparator() + System.lineSeparator() );

            for( ArgumentEntry missingEntry : missingArguments ) {
                errorBuilder.append( missingEntry.getLongName() ).append( ":" + System.lineSeparator() ).append( missingEntry.getDescription() + System.lineSeparator() );
            }

            throw new Exception( errorBuilder.toString() );
        }


        alreadyParsed = true;
        return parsedArguments;
    }


    /**
     * This class represents individual input argument and encapsulates expected properties of each one. It also defines
     * validation rules for a particular argument.
     */

    public static class ArgumentEntry {
        private String shortName;
        private String longName;
        private String defaultValue;
        private String description;
        private boolean _isFlag;
        private boolean _isOptional;
        private boolean isHelp;
        private Predicate< String > validationPredicate;

        public boolean isHelp() {
            return isHelp;
        }


        public String getShortName() {
            return shortName;
        }

        public boolean isFlag() {
            return _isFlag;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean isOptional() {
            return _isOptional;
        }

        public String getDescription() {
            return description;
        }

        public String getLongName() {
            return longName;
        }

        public boolean validate( String args ) throws Exception {

            return validationPredicate.test( args );
        }

        /**
         * Creates new instance of {@link ArgumentEntry}
         *
         * @param shortName           short-form name of the argument
         * @param longName            long-form name of the argument
         * @param description         argument description. This message is used upon showing help message, or creating error message
         *                            in case of failed validation
         * @param isOptional          determines whether the argument is optional or mandatory
         * @param isFlag              determines whether the argument is a flag type, i.e. it is used as an indicator with no values
         *                            associated to it
         * @param defaultValue        default value for the argument
         * @param validationPredicate validation rules for argument
         * @param isHelp              determines this argument is used to show help/usage message. If set, all other
         *                            arguments in the argument list are ignored and help/usage message is shown.
         */
        public ArgumentEntry( String shortName, String longName, String description, boolean isOptional,
                              boolean isFlag, String defaultValue, Predicate< String > validationPredicate,
                              boolean isHelp) {
            this.shortName = shortName;
            this.longName = longName;
            this._isFlag = isFlag;
            this.defaultValue = defaultValue;
            this._isOptional = isOptional;
            this.description = description;
            this.validationPredicate = validationPredicate;
            this.isHelp = isHelp;
        }

    }
}