package com.sybyl.trace.location;

public enum Location {
    KENYA,
    TANZANIA;
  
    
    /** Nice label for dropdowns */
    public String label() {
        return switch (this) {
            case KENYA -> "Kenya";
            case TANZANIA -> "Tanzania";
        };
    }
}
