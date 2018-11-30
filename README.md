This is a tool for reverse engineering android apps.

It reads all resources from res/values/ and searches in the smali code for occurrences of a constant definition of the resource id and prepends a comment with the value of the resource.

---
Known bugs:
- generated comments are always intended with 4 spaces but there are rare cases where it should be intended with 8 (or even more) spaces
