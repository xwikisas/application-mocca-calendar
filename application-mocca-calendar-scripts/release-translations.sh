#!/bin/bash
BRANCH=master
USER="$L10N_USER"
PASS="$L10N_PASSWORD"


my_dir=$(dirname $0)
my_dir=$(cd $my_dir > /dev/null; pwd)
XWIKI_TRUNKS=$my_dir/../..

# parse command line options
force=""
verbose="y"
while getopts "fq" opt; do
   case $opt in
      f) force="y"
         ;;
      q) verbose=""
         ;;
      \?) 
         echo "$0: unknown parameter found. STOP.";
         exit -1;
    esac
done
shift $(($OPTIND-1))

# TODO: set the proper java version !
# TODO: xwiki-settings.xml location is hard wired here
MVN=${MVN:-xwiki5_mvn}
function xwiki5_mvn() {
   MAVEN_OPTS="-Xmx4096m -XX:MaxPermSize=4096m";
   MAVEN_HOME=$HOME/opt/apps/apache-maven-3.0.4;
   $MAVEN_HOME/bin/mvn -s $HOME/work/xwiki/src/xwiki-settings.xml "$@";
}

function fix_author() {
    find ./ -name '*.xml' -exec sed -i -e 's#<creator>XWiki.Admin</creator>#<creator>xwiki:XWiki.Admin</creator>#' -e 's#<author>XWiki.Admin</author>#<author>xwiki:XWiki.Admin</author>#' -e 's#<contentAuthor>XWiki.Admin</contentAuthor>#<contentAuthor>xwiki:XWiki.Admin</contentAuthor>#' -e 's#<parent>L10NCode.MoccaCalendarClass</parent>#<parent>MoccaCalendarClass</parent>#' {} \; -print
}

function do_one() {
    wget $1 --user="${USER}" --password="${PASS}" --auth-no-challenge -O ./translations.zip &&
    unzip -o translations.zip &&
    rm translations.zip || $(git clean -dxf && exit -1)
    fix_author
}

function read_user_and_password() {
    if [[ -z "$USER" || -z "$PASS" ]]; then
        echo -e "\033[0;32mEnter your l10n.xwiki.org credentials:\033[0m"
        read -e -p "user> " USER
        read -e -s -p "pass> " PASS
        echo ""
    fi

    if [[ -z "$USER" || -z "$PASS" ]]; then
      echo -e "\033[1;31mPlease provide both user and password in order to be able to get the translations from l10n.xwiki.org.\033[0m"
      exit -1
    fi
}

function format_xar() {
    ## due to https://github.com/mycila/license-maven-plugin/issues/37 we need to perform "mvn xar:format" twice.
    $MVN xar:format
    $MVN xar:format
}

function do_all() {
    read_user_and_password

    ##
    ## Mocca Calendar UI
    ##

    cd ${XWIKI_TRUNKS}/application-mocca-calendar/application-mocca-calendar-ui/src/main/resources/ || exit -1
    do_one 'http://l10n.xwiki.org/xwiki/bin/view/L10NCode/GetTranslationFile?name=Contrib.MoccaCalendar&app=Contrib'
    cd ${XWIKI_TRUNKS}/application-mocca-calendar/application-mocca-calendar-ui/ && format_xar
 
    ## properties file looks like:
    ## 
    #cd ${XWIKI_TRUNKS}/xwiki-commons/xwiki-commons-core/xwiki-commons-extension/xwiki-commons-extension-api/src/main/resources/ || exit -1
    # do_one 'http://l10n.xwiki.org/xwiki/bin/view/L10NCode/GetTranslationFile?name=Commons.xwiki-commons-extension-api&app=Commons'


    git status
    echo -e "\033[0;32mIf there are untracked files, something probably went wrong.\033[0m"
}

function check_clean() {
    cd ${XWIKI_TRUNKS}/$1
    if [[ "`git status | grep 'nothing to commit (working directory clean)'`" == "" ]]; then
        git status
        echo -e "\033[1;31mPlease do something with these changes first.\033[0m"
        echo "in `pwd`"
        exit -1;
    fi
    git reset --hard &&
    git checkout ${BRANCH} &&
    git reset --hard &&
    git clean -dxf &&
    git pull origin ${BRANCH} || exit -1
}

function commit() {
    MSG="[release] Updated translations."
    cd ${XWIKI_TRUNKS}/application-mocca-calendar/
    git add . && git commit  -m "${MSG}" && git push
}

if [[ $1 == 'commit' ]]; then
    commit
elif [[ $1 == 'clean' ]]; then
    cd ${XWIKI_TRUNKS}/application-mocca-calendar
    git reset --hard && git clean -dxf
else
    [ -n "$force" ] || check_clean application-mocca-calendar
    if [ -z "$verbose" ]; then
      do_all > /dev/null
    else
      do_all
    fi
fi
