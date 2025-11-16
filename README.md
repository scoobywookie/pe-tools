# pe-tools

This project was upgraded to Java 21 (LTS).

Quick start (Windows cmd.exe):

1. Install JDK 21 and set `JAVA_HOME`:

```
setx JAVA_HOME "C:\\path\\to\\jdk-21"
setx PATH "%JAVA_HOME%\\bin;%PATH%"
```

2. Build the project:

```
mvn -U clean package
```

3. Run the JavaFX app using the plugin:

```
mvn javafx:run
```

Notes:
- The `pom.xml` has been updated to use `<java.version>21</java.version>` and JavaFX 21 with a platform classifier (default `win`). Change `<javafx.platform>` to `linux` or `mac` as needed.
- CI is included to build on Java 21.
