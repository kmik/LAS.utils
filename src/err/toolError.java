package err;

public class toolError extends RuntimeException {

    public toolError(String errorMessage) {
        super(errorMessage);
    }

}
