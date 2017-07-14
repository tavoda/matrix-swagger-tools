# matrix-swagger-tools
##Swagger tools for matrix messaging system - maybe useful also for others
Tools for converting files From/To Yaml and JSON formats and tools for merging
separate file based swagger definitions to one huge swagger file.

## Build
> mvn clean install
## Run
> java -jar ~/.m2/repository/org/matrix/swagger/matrix-swagger-tools/1.0-SNAPSHOT/matrix-swagger-tools-1.0-SNAPSHOT-jar-with-dependencies.jar

Usage: Tool [options] [command] [command options]
  Commands:
    toYaml      Convert file to yaml
      Usage: toYaml [options] <Input JSON or YAML file>
        Options:
          -h, --help
            Print help
          -o, --output-file
            Output file, if not specified calculated automatically
          -v, -d, --verbose, --debug
            Debug/verbose mode
            Default: false

    toJson      Convert file to JSON
      Usage: toJson [options] <Input JSON or YAML file>
        Options:
          -h, --help
            Print help
          -o, --output-file
            Output file, if not specified calculated automatically
          -v, -d, --verbose, --debug
            Debug/verbose mode
            Default: false

    merge      Merge swagger definitions from multiple files to one
      Usage: merge [options] <Swagger definition directory>
        Options:
          -h, --help
            Print help
          -o, --output-file
            Output file, if not specified calculated automatically
          -v, -d, --verbose, --debug
            Debug/verbose mode
            Default: false

    smartMerge      Merge swagger definitions from multiple files to one
      Usage: smartMerge [options] <Swagger API definition files>
        Options:
          -h, --help
            Print help
          -o, --output-file
            Output file, if not specified calculated automatically
            Default: /tmp/output.json
          -v, -d, --verbose, --debug
            Debug/verbose mode
            Default: false

    parse      Parse Swagger definition file and print all values recursively
      Usage: parse [options] <Swagger definition file>
        Options:
          -h, --help
            Print help
          -v, -d, --verbose, --debug
            Debug/verbose mode
            Default: false


