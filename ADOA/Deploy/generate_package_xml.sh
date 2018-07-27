# generate_package_xml.sh <workspace directory>
#get list of modified files

export WSPACE=$1

dos2unix $WSPACE/version.txt 2>>/dev/null

version=$(cat $WSPACE/version.txt)
git diff --oneline --name-only $version HEAD | grep -v version.txt |grep -v "Referenced Packages" > $WSPACE/modified_files
sed -i 's/Package xmlns="http:\/\/soap.sforce.com\/2006\/04\/metadata"/Package/g' $WSPACE/src/package.xml


for CFILE in `cat modified_files`
do
        echo Analyzing file `basename $CFILE`

        case "$CFILE"
                in
                        *.cls*) TYPENAME="ApexClass";;
                        *.page*) TYPENAME="ApexPage";;
                        *.component*) TYPENAME="ApexComponent";;
                        *.trigger*) TYPENAME="ApexTrigger";;
                        *.app*) TYPENAME="CustomApplication";;
                        *.labels*) TYPENAME="CustomLabels";;
                        *.object*) TYPENAME="CustomObject";;
                        *.tab*) TYPENAME="CustomTab";;
                        *.resource*) TYPENAME="StaticResource";;
                        *.workflow*) TYPENAME="Workflow";;
                        *.remoteSite*) TYPENAME="RemoteSiteSettings";;
                        *.pagelayout*) TYPENAME="Layout";;
                        *) TYPENAME="UNKNOWN TYPE";;
        esac

                if [[ "$TYPENAME" != "UNKNOWN TYPE" ]]
                then
                        ENTITY=$(basename "$CFILE")
                        ENTITY="${ENTITY%.*}"
                        echo ENTITY NAME: $ENTITY

                        if grep -Fq "$TYPENAME" $WSPACE/src/package.xml
                        then
                                echo Generating new member for $ENTITY
                                xmlstarlet ed -L -s "/Package/types[name='$TYPENAME']" -t elem -n members -v "$ENTITY" $WSPACE/src/package.xml
                        else
                                echo Generating new $TYPENAME type
                                xmlstarlet ed -L -s /Package -t elem -n types -v "" $WSPACE/src/package.xml
                                xmlstarlet ed -L -s '/Package/types[not(*)]' -t elem -n name -v "$TYPENAME" $WSPACE/src/package.xml
                                echo Generating new member for $ENTITY
                                xmlstarlet ed -L -s "/Package/types[name='$TYPENAME']" -t elem -n members -v "$ENTITY" $WSPACE/src/package.xml
                        fi
                else
                        echo ERROR: UNKNOWN FILE TYPE $CFILE
                fi
done

echo ====UPDATED PACKAGE.XML====
sed -i 's/<Package>/<Package xmlns="http:\/\/soap.sforce.com\/2006\/04\/metadata">/g' $WSPACE/src/package.xml
cat $WSPACE/src/package.xml