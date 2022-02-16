#!/bin/bash

# trap 'catch' ERR

# catch() {
#   echo "An error has occurred but we're going to eat it!!"
# }

TEST_MODULES="http-log,timer-log"
TEST_FAILURES=()
RED='\033[0;31m'
CLEAR='\033[0m'

# for MODULE in ${TEST_MODULES//,/ }; do
# ./mvnw ${MAVEN_ARGS} clean verify \
#     -Dformatter.skip -Dimpsort.skip \
#     -f "${MODULE}/pom.xml"


# if [[ $? -ne 0 ]]; then
#     TEST_FAILURES[${#TEST_FAILURES[@]}]=${MODULE}
# fi

# done

# if [[ ${#TEST_FAILURES[@]} -gt 0 ]]; then
#     echo -e "\n\n${RED}Build errors were encountred in the following projects:${CLEAR}\n\n"
#     for FAILURE in ${TEST_FAILURES[@]}; do
#         echo -e "* ${RED}${FAILURE}${CLEAR}"
#     done
#     echo -e "\n\n${RED}Check build logs for further information.${CLEAR}"
# fi



for i in "foo bar cheese"; do
echo -n "$i\n"
done