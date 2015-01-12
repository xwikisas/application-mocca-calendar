
Helper Scripts for development
==============================

release-translations.sh
-----------------------

This script downloads the translations from l10n.xwiki.org and applies
them to the local git repository. When it is finished, it prints all of changes
using git-diff. If called without argument it does *not* do any commit or push.
If called with `commit` as argument, it commits the current local changes, no matter what they are.

This is a copy of the script at https://github.com/xwiki/xwiki-dev-tools/blob/master/xwiki-release-scripts/staged/release-translations.sh

Example:

    cd /path/to/xwiki-contrib/application-mocca-calendar
    cd application-mocca-calendar-scripts/
    ~/release-translations.sh
    cd ..
    git commit -m "updating translations from l10n.xwiki.org"
    git push

If there are any uncommitted changes to the repository, the script will fail.
To remove all uncommitted changes in **all repositories**, use

    cd ./xwiki-trunks
    ~/release-translations.sh clean

You migth want to set the environment variables `L10N_USER` and `L10N_PASSWORD`
or the script will ask you for your username/password on l10n.xwiki.org.

Bug: the "xwiki5_mvn" helper function uses a hard wired path,
you migth want to edit the script to adapt to your needs.
Alternatively you can set the environment variable `MVN` to point to the maven executable to format the 