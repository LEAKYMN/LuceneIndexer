# This script wont work if it is not made executable with 'sudo chmod +x
cd ../../LuceneIndexer/
cp -f -r lib dist/lib
java -jar dist/LuceneIndexer.jar
