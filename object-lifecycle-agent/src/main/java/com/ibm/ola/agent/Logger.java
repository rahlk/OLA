package com.ibm.ola.agent;

public class Logger {
	
	public static enum Level {
		Trace(0, "Trace"),
        Info(1, "Info"),
		Warn(2, "Warn"),
		Error(3, "Error");
		
		public final int index;
		public final String text;
		
		Level(int index, String text) {
			this.index = index;
			this.text = text;
		}
		
		public boolean isFiltered(Level level) {
			return level.index < index;
		}
		
		public static Level levelValue(String level) {
			Level levels[] = values();
			for (int i=0; i<levels.length; i++) 
				if (levels[i].text.equalsIgnoreCase(level)) return levels[i];
			
			return null;
		}
		
		public String toString() {
		    return text;
		}
	}
	
	static Level level = Level.Warn; 

	private boolean isEnabled(Level level) {
		return !Logger.level.isFiltered(level);
	}
	
	public boolean isTraceEnabled() {
		return isEnabled(Level.Trace);
	}
	
	public boolean isWarnEnabled() {
		return isEnabled(Level.Warn);
	}

    public boolean isInfoEnabled() {
        return isEnabled(Level.Info);
    }
	
	public boolean isErrorEnabled() {
		return isEnabled(Level.Error);
	}
	
	public Level getLevel() {
		return level;
	}
	
	public synchronized static void setLevel(Level level) {
		Logger.level = level;
	}
	
	public static void setLevel(Configuration configuration) {
		String s = configuration.getStringValue("log-level");
		Level level = Level.levelValue(s);
		if (level != null) setLevel(level);
	}
	
	public void trace(String message) {
	    output(Level.Trace, message, null);
	}

    public void trace(String message, Throwable t) {
        output(Level.Trace, message, t);
    }
    
    public void info(String message) {
        output(Level.Info, message, null);
    }

    public void info(String message, Throwable t) {
        output(Level.Info, message, t);
    }
	
	public void warn(String message) {
	    output(Level.Warn, message, null);
	}
	
	public void warn(String message, Throwable t) {
	    output(Level.Warn, message, t);
	}
	
	public void error(String message) {
	    output(Level.Error, message, null);
	}
	
	public void error(String message, Throwable t) {
		output(Level.Error, message, t);
	}
	
    public void output(Level level, String message, Throwable t) {
        if (isEnabled(level)) {
            System.err.println(level + ": " + message);
            if (t != null) t.printStackTrace(System.err);
        }
    }
	
}
