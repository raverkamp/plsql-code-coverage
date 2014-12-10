plsql-codecoverage

plsqlcodecoverage is a program to do code coverages of Oracle PL/SQL code
running in the database. Currently only code coverage for packages is
supported.

The program creates the following database objects:
the tables 
aaa_coverage
aaa_coverage_statements

the sequence 
aaa_coverage_seq

and the package 
aaa_coverage_tool

The codecoverage process works by instrumenting the PL/SQL code, a logging
statement is placed before each statement.

Example:
A function
function f2(x varchar2) return varchar2 is
begin
  return 'd'||x;
end;

becomes

function f2(x varchar2) return varchar2 is
begin
  "$log"(4);return 'd'||x;
end;

The execution of the statments is logged into the tables aaa_coverage and
aaa_coverage_statements. Only the first execution of a statement is logged.
The original package state is stored in a clob in table aaa_coverage.

The codecoverage process is started for each package individually.
The codecoverage process works for all sessions that use the instrumented 
packages.

You can only do code coverage for the packages the database user owns.
It is possible to use a proxy user.

It should be obvious by know that, that the user needs full rights for
the schema, the user has to create/drop tables and packages.

Each schema/user has its own version of the database objects.

How to build

plsqlodecoverage is currently developed as a netbeans project.
The platform is java 7, but the root classes are still java 8. 
I use this setup for developing/debugging.
 
The system can also be build with a simple ant build file:
build-plsqlcodecoverage.xml. 

On windows you need an ant installation and you have to
set JAVA_HOME correctly. I do this in a configuration script
which has these contents on my machine:

set java_home=C:\Program Files\Java\jdk1.8.0_25
set ant="C:\Program Files\NetBeans 8.0.1\extide\ant\bin\ant.bat"
set orajdbclib=C:\oraclexe\app\oracle\product\11.2.0\server\jdbc\lib\ojdbc6.jar
set jre7=C:\Program Files (x86)\Java\jre7

The variables orajdbclib is used in the build for obvious reasons.
The variable jre7 is used to set the bootclasspath.

run
%ant% -f build-plsqlcodecoverage.xml dist|dist2
to build the system.
The target dist creates a jar file psqlcodecoverage.jar in the directory dist
and the folder lib in the same directory contains the file ojdbc6.jar
which is referenced in the manifest of the file psqlcodecoverage.jar.

The target dist2 creates a jar file psqlcodecoverage.jar in the directory dis, 
this jar file contains the class files of the project as well as the class files
of the orcale jdbc library.

run the program with
java -jar psqlcodecoverage.jar






