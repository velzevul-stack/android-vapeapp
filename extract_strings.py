import re

def extract(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    strings = re.findall(r'"([^"\n]*[^"\n]*)"', content)
    for s in set(strings):
        print(f'"{s}"')

print("AcceptScreen.kt:")
extract("app/src/main/java/com/example/vapestoreapp/ui/screens/AcceptScreen.kt")
print("\nSellScreen.kt:")
extract("app/src/main/java/com/example/vapestoreapp/ui/screens/SellScreen.kt")
