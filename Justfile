help:
    just --list

generate_points_file:
    java Points.java

compile:
    rm -rf build



    mkdir -p build/value_src
    cp -r src/main build/value_src
    find ./build/value_src -type f -name "*.java" -exec sed -i '' 's|/\*value\*/|value|g' {} +

    /Users/emccue/Downloads/jdk-28.jdk/Contents/Home/bin/javac \
      -p lib -d build/javac $(find ./src/main -name "*.java" -type f)

    /Users/emccue/Downloads/jdk-28.jdk/Contents/Home/bin/javac \
      --enable-preview --source 28 \
      -p lib -d build/value_javac $(find ./build/value_src -name "*.java" -type f)


    time /Users/emccue/Downloads/jdk-28.jdk/Contents/Home/bin/java \
      -p lib -cp build/javac --add-modules ALL-MODULE-PATH src/test/java/PointDemo.java

    time /Users/emccue/Downloads/jdk-28.jdk/Contents/Home/bin/java \
      --enable-preview -p lib -cp build/value_javac --add-modules ALL-MODULE-PATH src/test/java/PointDemo.java