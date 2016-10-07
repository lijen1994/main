package tars.model;

import javafx.collections.transformation.FilteredList;
import tars.commons.core.ComponentManager;
import tars.commons.core.LogsCenter;
import tars.commons.core.UnmodifiableObservableList;
import tars.commons.events.model.TarsChangedEvent;
import tars.commons.util.StringUtil;
import tars.model.task.Task;
import tars.model.task.ReadOnlyTask;
import tars.model.task.UniqueTaskList;
import tars.model.task.UniqueTaskList.TaskNotFoundException;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents the in-memory model of tars data.
 * All changes to any model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final Tars tars;
    private final FilteredList<Task> filteredTasks;

    /**
     * Initializes a ModelManager with the given Tars
     * Tars and its variables should not be null
     */
    public ModelManager(Tars src, UserPrefs userPrefs) {
        super();
        assert src != null;
        assert userPrefs != null;

        logger.fine("Initializing with address book: " + src + " and user prefs " + userPrefs);

        tars = new Tars(src);
        filteredTasks = new FilteredList<>(tars.getTasks());
    }

    public ModelManager() {
        this(new Tars(), new UserPrefs());
    }

    public ModelManager(ReadOnlyTars initialData, UserPrefs userPrefs) {
        tars = new Tars(initialData);
        filteredTasks = new FilteredList<>(tars.getTasks());
    }

    @Override
    public void resetData(ReadOnlyTars newData) {
        tars.resetData(newData);
        indicateTarsChanged();
    }

    @Override
    public ReadOnlyTars getTars() {
        return tars;
    }

    /** Raises an event to indicate the model has changed */
    private void indicateTarsChanged() {
        raise(new TarsChangedEvent(tars));
    }

    @Override
    public synchronized void deleteTask(ReadOnlyTask target) throws TaskNotFoundException {
        tars.removeTask(target);
        indicateTarsChanged();
    }

    @Override
    public synchronized void addTask(Task task) throws UniqueTaskList.DuplicateTaskException {
        tars.addTask(task);
        updateFilteredListToShowAll();
        indicateTarsChanged();
    }

    //=========== Filtered Task List Accessors ===============================================================

    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredTaskList() {
        return new UnmodifiableObservableList<>(filteredTasks);
    }

    @Override
    public void updateFilteredListToShowAll() {
        filteredTasks.setPredicate(null);
    }

    @Override
    public void updateFilteredTaskList(Set<String> keywords){
        updateFilteredTaskList(new PredicateExpression(new NameQualifier(keywords)));
    }

    private void updateFilteredTaskList(Expression expression) {
        filteredTasks.setPredicate(expression::satisfies);
    }

    //========== Inner classes/interfaces used for filtering ==================================================

    interface Expression {
        boolean satisfies(ReadOnlyTask task);
        String toString();
    }

    private class PredicateExpression implements Expression {

        private final Qualifier qualifier;

        PredicateExpression(Qualifier qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public boolean satisfies(ReadOnlyTask task) {
            return qualifier.run(task);
        }

        @Override
        public String toString() {
            return qualifier.toString();
        }
    }

    interface Qualifier {
        boolean run(ReadOnlyTask task);
        String toString();
    }

    private class NameQualifier implements Qualifier {
        private Set<String> nameKeyWords;

        NameQualifier(Set<String> nameKeyWords) {
            this.nameKeyWords = nameKeyWords;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            return nameKeyWords.stream()
                    .filter(keyword -> StringUtil.containsIgnoreCase(task.getName().taskName, keyword))
                    .findAny()
                    .isPresent();
        }

        @Override
        public String toString() {
            return "name=" + String.join(", ", nameKeyWords);
        }
    }

}