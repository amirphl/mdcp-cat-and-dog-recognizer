# mdcp cat and dog recognizer

## how to build jar and dex
- `mvn clean install`
- `/usr/lib/android-sdk/build-tools/30.0.3/d8 --release --output target/mdcp-cat-and-dog-recognizer-dex.jar target/mdcp-cat-and-dog-recognizer.jar`

## program arguments
- arg 0: input file URL ---> url of a file containing image urls (cat and dog images) [sample input](input-sample.txt)
- arg 1: output file path ---> program writes recognition result of images in this path [sample output](output.csv)
- arg 2: fraction ---> fraction of input to perform recognition (not yet used) 
- arg 3: total fractions (not yet used)
- ex: example.com/input.txt /home/user/output.out 3 10
here 3 10 means we want to recognize 3/10 of input

## output format
url # result(0 -> dog, 1 > cat, -1 -> unknown).  
- ex1: http://localhost:7979/static/images/dog2.jpg#0
- ex2: http://localhost:7979/static/images/cat4.jpg#1
