## Setup

* Clone repo

* Install souffle using following commands (modify config.ac to modify install directory)
    
      sudo apt-get install autoconf automake bison build-essential clang doxygen flex g++ git libncurses5-dev libtool libsqlite3-dev make mcpp python sqlite zlib1g-dev
      cd souffle
      sh ./bootstrap
      ./configure
      make
      PATH=$PATH:<install-dir>/bin

* Switch to doop-framework (not necessary to put souffle and doop in same directory)

* Run (can also use example.jar provided in docs/doop-101-examples as a starter) 

      ./doop -a dependency-analysis -i samples/irs.jar -id irs_dependency_analysis


## Analysis
Path "doop-folder/out/analysis-name/id-for-analysis" contains results of all analysis run so far. "doop-folder/last-analysis" holds a symbolic link to database of last-run analysis. 

### Static call graph generation
* After successfully running analysis, issue the following command to generate static call graph (inside doop-framework)

        python build-graph.py last-analysis/ApplicationMethod.csv last-analysis/CallGraphEdge.csv 0

* IRS call graph without context labels over edge ![alt text](https://github.com/rahlk/OLA/blob/main/doop/analysis/irs_call_graph_without_context.png)
* IRS call graph with context labels over edge ![alt text](https://github.com/rahlk/OLA/blob/main/doop/analysis/irs_call_graph_with_context.png)

### Var_Points_To
* Each analysis stores variable pointers information in VarPoints.csv
* Run following commands to find variable-points-to information for a specific method
          
           cp out/analysis-name/$id/facts/Var-DeclaringMethod.facts out/analysis-name/$id/database
           souffle -F out/analysis-name/$id/database temp_dl.logic
           cat Temp.csv

* Attahcing few output samples after running dependency analysis on IRS.jar as follows
#### BusinessProcess: void main(java.lang.String[])
    <BusinessProcess: void main(java.lang.String[])>/@parameter0	<<main method array>>
    <BusinessProcess: void main(java.lang.String[])>/$stack2	<BusinessProcess: void main(java.lang.String[])>/new BusinessProcess/0
    <BusinessProcess: void main(java.lang.String[])>/instBP#_108	<BusinessProcess: void main(java.lang.String[])>/new BusinessProcess/0
    <BusinessProcess: void main(java.lang.String[])>/args#_0	<<main method array>>

#### IRS: Salary getSalaryfromIRS(int)
    <IRS: Salary getSalaryfromIRS(int)>/@this	<BusinessProcess: void test_2()>/new IRS/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack4	<java.lang.System: java.io.PrintStream newPrintStream(java.io.FileOutputStream,java.lang.String)>/new java.io.PrintStream/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack4	<<null pseudo heap>>
    <IRS: Salary getSalaryfromIRS(int)>/this#_0	<BusinessProcess: void test_2()>/new IRS/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack5	<BusinessProcess: void test_2()>/new java.util.ArrayList/0
    <IRS: Salary getSalaryfromIRS(int)>/l3#_33	<java.util.ArrayList: java.util.Iterator iterator()>/new java.util.ArrayList$Itr/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack8	<BusinessProcess: void genSalarySlip(IRS)>/new Salary/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack8	<BusinessProcess: void test_2()>/new Salary/0
    <IRS: Salary getSalaryfromIRS(int)>/salary#_33	<BusinessProcess: void genSalarySlip(IRS)>/new Salary/0
    <IRS: Salary getSalaryfromIRS(int)>/salary#_33	<BusinessProcess: void test_2()>/new Salary/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack9	<java.lang.System: java.io.PrintStream newPrintStream(java.io.FileOutputStream,java.lang.String)>/new java.io.PrintStream/0
    <IRS: Salary getSalaryfromIRS(int)>/$stack9	<<null pseudo heap>>
    <IRS: Salary getSalaryfromIRS(int)>/$stack10	<<string-constant>>
    <IRS: Salary getSalaryfromIRS(int)>/$stack10	<java.lang.StringBuilder: java.lang.String toString()>/new java.lang.String/0
    <IRS: Salary getSalaryfromIRS(int)>/$stringconstant0	<<string-constant>>
    <IRS: Salary getSalaryfromIRS(int)>/$null0	<<null pseudo heap>>

#### Employee: int getEmployeeId()
    <Employee: int getEmployeeId()>/@this	<BusinessProcess: java.util.List getAllEmployers()>/new Employee/0
    <Employee: int getEmployeeId()>/@this	<BusinessProcess: java.util.List getAllEmployers()>/new Employee/1
    <Employee: int getEmployeeId()>/@this	<BusinessProcess: void test_2()>/new Employee/0
    <Employee: int getEmployeeId()>/$stack1	<java.lang.System: java.io.PrintStream newPrintStream(java.io.FileOutputStream,java.lang.String)>/new java.io.PrintStream/0
    <Employee: int getEmployeeId()>/$stack1	<<null pseudo heap>>
    <Employee: int getEmployeeId()>/this#_0	<BusinessProcess: java.util.List getAllEmployers()>/new Employee/0
    <Employee: int getEmployeeId()>/this#_0	<BusinessProcess: java.util.List getAllEmployers()>/new Employee/1
    <Employee: int getEmployeeId()>/this#_0	<BusinessProcess: void test_2()>/new Employee/0
    <Employee: int getEmployeeId()>/$stringconstant0	<<string-constant>>
    

### Information flow analysis (taint analysis)
* Taint analysis can be performed in various ways
* Provides options for leveraging object-based information propogation, call-site based information propogation
* Taint/information flow analysis can be triggered with any analysis by using flag `--information-flow` with `minimal` as flag value
      
      ./doop -a 2-object-sensitive+heap -i samples/irs.jar -id irs_dependency_analysis --information-flow minimal

* IRS.jar returns empty files for tainted flow analysis
* Suppose following class information (`samples/Main.jar`)

        import java.io.BufferedReader;
        import java.io.PrintWriter;
        import java.io.InputStreamReader;
        import java.io.IOException;

        public class Main {
            public void bad() throws IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter writer = new PrintWriter(System.out);

                String data = reader.readLine();
                data = data.replace("foo", "bar");
                writer.println(data);
            }

            public void good() {
                PrintWriter writer = new PrintWriter(System.out);

                String data = "safe string";
                data = data.replace("foo", "bar");
                writer.println(data);
            }

            public static void main(String[] args) throws IOException {
                (new Main()).bad();
                (new Main()).good();
            }
        }

* Running information-flow analysis for above jar will yeild following result in `last-analysis/LeakingTaintedInformation.csv`

        default default [<<immutable-context>>	<Main: void main(java.lang.String[])>/new Main/0] <Main: void bad()>/java.io.PrintWriter.println/0 <Main: void bad()>/java.io.BufferedReader.readLine/0

* Can also define custom source and sink methods
