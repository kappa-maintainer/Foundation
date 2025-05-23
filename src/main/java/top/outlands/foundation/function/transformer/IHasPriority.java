package top.outlands.foundation;

public interface IHasPriority{

    /**
     * Override this to set your transformer's priority.
     * @return The priority int. 
     */
    default int getPriority() {
        return 0;
    }
}