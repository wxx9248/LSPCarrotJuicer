#include <string.h>

// Function to check if a string ends with a given suffix
int endsWith(const char *string, const char *suffix) {
    int stringLength = strlen(string);
    int suffixLength = strlen(suffix);

    // If the suffix is longer than the string, it can't be a suffix
    if (stringLength < suffixLength)
        return 0;

    // Compare the end of the string with the suffix
    return strcmp(string + stringLength - suffixLength, suffix) == 0;
}
