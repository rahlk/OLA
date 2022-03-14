# JAVA ASM agents for call tracing and object mining

_TODO: Say what, why, how, etc._

There are three sub-projects here.

```
------------------------------------------------------------
Root project
------------------------------------------------------------

Root project 'OLA'
    +--- Project ':call-tracing-agent'       --> This agent instruments classfile to extract caller-callee relationships 
                                                 (optionally saves the bytecode of the arguments, and also appends 
                                                 context sensitivity based on https://arxiv.org/abs/1905.02088)
    +--- Project ':object-lifecycle-agent'   --> This agent instruments tracks the objects saved on the heap and 
                                                 generates lifecycle information (i.e., time between allocation and 
                                                 garbage collections) in a json file. Inspired by https://9m.no/ﷄ뺏
    +--- Project ':test'                     --> This are just a set of test programs to test the working of above 
                                                 agents. Inspired by and extended from https://9m.no/䎁᎑
```

## Usage

- To use these agents, build using
  
  ```sh
  ./gradlew clean fatJar
  ```

- To attach the agent to a jar called TEST.jar, assumming your classpath environment variable is $CLASSPATH, use

  ```sh
  java -javaagent:/path/to/OLA/call-tracing-agent/build/libs/ctaagent-1.0-SNAPSHOT.jar -cp /path/to/OLA/call-tracing-agent/build/libs/ctaagent-1.0-SNAPSHOT.jar:$CLASSPAT -jar /path/to/TEST.jar
  ```
