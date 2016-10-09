package tars.logic.parser;

import tars.commons.core.Messages;
import tars.commons.exceptions.IllegalValueException;
import tars.commons.util.StringUtil;
import tars.logic.commands.AddCommand;
import tars.logic.commands.ClearCommand;
import tars.logic.commands.Command;
import tars.logic.commands.DeleteCommand;
import tars.logic.commands.ExitCommand;
import tars.logic.commands.FindCommand;
import tars.logic.commands.HelpCommand;
import tars.logic.commands.IncorrectCommand;
import tars.logic.commands.ListCommand;
import tars.logic.commands.SelectCommand;

import static tars.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tars.commons.core.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.time.DateTimeException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses user input.
 */
public class Parser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)");

    private static final Pattern PERSON_INDEX_ARGS_FORMAT = Pattern.compile("(?<targetIndex>.+)");

    private static final Pattern KEYWORDS_ARGS_FORMAT =
            Pattern.compile("(?<keywords>\\S+(?:\\s+\\S+)*)"); // one or more keywords separated by whitespace
    
    private static final String FLAG_DATETIME = "-dt";
    private static final String FLAG_PRIORITY = "-p";
    private static final String FLAG_TAG = "-t";

    public Parser() {}

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     */
    public Command parseCommand(String userInput) {
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(userInput.trim());
        if (!matcher.matches()) {
            return new IncorrectCommand(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");
        switch (commandWord) {

        case AddCommand.COMMAND_WORD:
            return prepareAdd(arguments);

        case SelectCommand.COMMAND_WORD:
            return prepareSelect(arguments);

        case DeleteCommand.COMMAND_WORD:
            return prepareDelete(arguments);

        case ClearCommand.COMMAND_WORD:
            return new ClearCommand();

        case FindCommand.COMMAND_WORD:
            return prepareFind(arguments);

        case ListCommand.COMMAND_WORD:
            return new ListCommand();

        case ExitCommand.COMMAND_WORD:
            return new ExitCommand();

        case HelpCommand.COMMAND_WORD:
            return new HelpCommand();

        default:
            return new IncorrectCommand(MESSAGE_UNKNOWN_COMMAND);
        }
    }

    /**
     * Parses arguments in the context of the add task command.
     *
     * @param args full command args string
     * @return the prepared command
     */
    private Command prepareAdd(String args) {
        String name = "";
        Option priorityOpt = new Option(FLAG_PRIORITY, true);
        Option dateTimeOpt = new Option(FLAG_DATETIME, true);
        Option tagOpt = new Option(FLAG_TAG, false);
        
        Option[] options = {
                priorityOpt, 
                dateTimeOpt, 
                tagOpt
        };
        
        TreeMap<Integer, Option> flagsPosMap = getFlagPos(args, options);
        HashMap<Option, String> optionFlagNArgMap = getOptionFlagNArg(args, options, flagsPosMap);
        
        if(flagsPosMap.size() == 0) {
            name = args;
        } else if (flagsPosMap.firstKey() == 0) {
         // name should be the first argument
            return new IncorrectCommand(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
        } else {
            name = args.substring(0, flagsPosMap.firstKey()).trim();
        }
        
        try {
            return new AddCommand(
                    name,
                    getDateTimeFromArgs(optionFlagNArgMap.get(dateTimeOpt).replace(FLAG_DATETIME + " ", "")),
                    optionFlagNArgMap.get(priorityOpt).replace(FLAG_PRIORITY + " ", ""),
                    getTagsFromArgs(optionFlagNArgMap.get(tagOpt)));
        } catch (IllegalValueException ive) {
            return new IncorrectCommand(ive.getMessage());
        } catch (DateTimeException dte) {
            return new IncorrectCommand(Messages.MESSAGE_INVALID_DATE);
        }
    }
    
    /**
     * Extracts the new task's datetime from the add command's task arguments string.
     */
    private static String[] getDateTimeFromArgs(String taskArguments) {
        if (taskArguments.equals("")) {
            return new String[] {"", ""};
        } else if (taskArguments.contains("to")) {
            int toIndex = taskArguments.indexOf("to");
            String startDateTime = taskArguments.substring(0, toIndex).trim();
            String endDateTime = taskArguments.substring(toIndex + 2).trim();
            return new String[] {startDateTime, endDateTime};
        } else {
            return new String[] {"", taskArguments.trim()};
        }
    }

    /**
     * Extracts the new task's tags from the add command's tag arguments string.
     * Merges duplicate tag strings.
     */
    private static Set<String> getTagsFromArgs(String tagArguments) throws IllegalValueException {
        // no tags
        if (tagArguments.equals("")) {
            return Collections.emptySet();
        }
        // replace first delimiter prefix, then split
        final Collection<String> tagStrings = Arrays.asList(tagArguments
                                                                .replaceFirst(FLAG_TAG + " ", "")
                                                                .split(" " + FLAG_TAG + " "));
        return new HashSet<>(tagStrings);
    }
    
    /**
     * Gets all flag position from arguments string
     */
    private static TreeMap<Integer, Option> getFlagPos(String args, Option[] options) {
        args = args.trim();
        TreeMap<Integer, Option> flagsPosMap = new TreeMap<Integer, Option>();
        
        if (args != null && args.length() > 0 && options.length > 0) {
            for (int i = 0; i < options.length; i++) {
                int indexOf = -1;
                do {
                    indexOf = args.indexOf(options[i].flag, indexOf + 1);
                    if (indexOf >= 0) {
                        flagsPosMap.put(indexOf, options[i]);
                    }
                    if (options[i].hasMultiple) {
                        break;
                    }
                } while (indexOf >= 0);
            }
        }
        
        return flagsPosMap;
    }
    
    /**
     * Extracts the option's flag and arg from arguments string.
     */
    private static HashMap<Option, String> getOptionFlagNArg(String args, Option[] options, TreeMap<Integer, Option> flagsPosMap) {
        args = args.trim();
        HashMap<Option, String> flagsValueMap = new HashMap<Option, String>();
        
        if (args != null && args.length() > 0 && options.length > 0) {
            // initialize the flagsValueMap
            for (int i = 0; i < options.length; i++) {
                flagsValueMap.put(options[i], "");
            }

            int endPos = args.length();
            for (Map.Entry<Integer, Option> entry : flagsPosMap.descendingMap().entrySet()) {
                Option option = entry.getValue();
                Integer pos = entry.getKey();
                
                if(pos == -1) {
                    continue;
                }

                String arg = args.substring(pos, endPos);
                endPos = pos;

                if (flagsValueMap.containsKey(option)) {
                    flagsValueMap.put(option, flagsValueMap.get(option).concat(" ").concat(arg));
                } else {
                    flagsValueMap.put(option, arg);
                }
            }
        }

        return flagsValueMap;
    }

    /**
     * Parses arguments in the context of the delete task command.
     *
     * @param args full command args string
     * @return the prepared command
     */
    private Command prepareDelete(String args) {

        Optional<Integer> index = parseIndex(args);
        if(!index.isPresent()){
            return new IncorrectCommand(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        }

        return new DeleteCommand(index.get());
    }

    /**
     * Parses arguments in the context of the select task command.
     *
     * @param args full command args string
     * @return the prepared command
     */
    private Command prepareSelect(String args) {
        Optional<Integer> index = parseIndex(args);
        if(!index.isPresent()){
            return new IncorrectCommand(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, SelectCommand.MESSAGE_USAGE));
        }

        return new SelectCommand(index.get());
    }

    /**
     * Returns the specified index in the {@code command} IF a positive unsigned integer is given as the index.
     *   Returns an {@code Optional.empty()} otherwise.
     */
    private Optional<Integer> parseIndex(String command) {
        final Matcher matcher = PERSON_INDEX_ARGS_FORMAT.matcher(command.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String index = matcher.group("targetIndex");
        if(!StringUtil.isUnsignedInteger(index)){
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(index));

    }

    /**
     * Parses arguments in the context of the find task command.
     *
     * @param args full command args string
     * @return the prepared command
     */
    private Command prepareFind(String args) {
        final Matcher matcher = KEYWORDS_ARGS_FORMAT.matcher(args.trim());
        if (!matcher.matches()) {
            return new IncorrectCommand(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                    FindCommand.MESSAGE_USAGE));
        }

        // keywords delimited by whitespace
        final String[] keywords = matcher.group("keywords").split("\\s+");
        final Set<String> keywordSet = new HashSet<>(Arrays.asList(keywords));
        return new FindCommand(keywordSet);
    }

}


class Option {
    public String flag;
    public boolean hasMultiple;

    public Option(String flag, boolean hasMultiple) {
        this.flag = flag;
        this.hasMultiple = hasMultiple;
    }
    
    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof Option // instanceof handles nulls
                && this.flag.equals(((Option) other).flag)); // state check
    }

    @Override
    public int hashCode() {
        return flag.hashCode();
    }
}
