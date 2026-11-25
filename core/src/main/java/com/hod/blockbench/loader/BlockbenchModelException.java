package com.hod.blockbench.loader;

import java.io.IOException;

public class BlockbenchModelException extends IOException {

    public BlockbenchModelException(String message) {
        super(message);
    }

    public BlockbenchModelException(String message, Throwable cause) {
        super(message, cause);
    }
}