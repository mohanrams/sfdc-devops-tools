import groovy.io.FileType

class PackageManager {

  static final String FILE_SEPARATOR = File.separator
  static final String VERSION_FILE_NAME = "version.txt"
  static final String DISTRIBUTION_FOLDER_DESTRUCTIVE = "dist_destructive"
  static final String EMPTY_PACKAGE_XML = """<?xml version="1.0" encoding="UTF-8"?>
      <Package xmlns="http://soap.sforce.com/2006/04/metadata">
      <version>30.0</version>
      </Package>
      """
  static final String[] META_DATA_NEEDED = ['.trigger','.resource','.site', '.cls', '.component', '.page', '.email', '.scf', '.certs']
  static final String META_DATA_SUFIX = '-meta.xml'
  static final String[] LIGHTNING_COMPONENTS = ['.cmp','.app','.design','.evt','.intf','.js','.css','.svg','.auradoc', '.tokens']


  /**
   * Directory where is store the sfdc-service code
   */
  String sourceDir
  /**
   * The name of the distribution folder for the main package
   */
  String destinationDir
  /**
   * The path where the distributions folders will be created
   */
  String baseDir
  boolean isDestructiveChangesPresent =false

  /**
   * Change a git repository to an specific branch
   */
  static def toBranch(repoPath,  branchName){

    def command = "git checkout ${branchName}"
    def repo = new File(repoPath)
    def proc = command.execute(null, repo)
    proc.waitFor()

    if (proc.exitValue())
      throw new java.lang.IllegalStateException("Error trying to use ${branchName}, please verify that the branch exists")

    println proc.in.text

  }

  /**
   * <p> Main method of the class, this method look in a directory that must contains the sfdc-service code
   * for any change since the version indicated on the version.txt, after retrieve the list of modified files
   * create a distribution folder that contains only the modified changes.</p>
   *
   * <p>If the modified file have one of the suffix listed on META_DATA_NEEDED variable, its corresponding meta 
   * data file also will be copied</p>
   */
  void createDeployPackage(){
    println "start"

   if(isDestructiveChangesPresent){
        createDestructivePkg()
        return
    }
    def version = getVersion()
    println "version: ${version}"

    def onlySRCQuery = { isOnSrc(it) }
    // get all modified files on src folder
    def modifiedFiles = getModifiedFiles(version).findAll(onlySRCQuery)

    println modifiedFiles.size()
    println modifiedFiles

    modifiedFiles.each {
      modifiedFile ->

      modifiedFile = modifiedFile.replace('/', "${FILE_SEPARATOR}")
      def source = "${sourceDir}${FILE_SEPARATOR}${modifiedFile}"
      def target = "${baseDir}${FILE_SEPARATOR}"+modifiedFile.replace("src", destinationDir)

      if( new File(source).exists()){

        createParentDir(target)
        println source
        println target
        (new AntBuilder()).copy(file: source, tofile: target)

        if(hasMetaData(source)){
        def sourcefile = new File(source)
        println sourcefile.getParent() 
         def sourcefolder = sourcefile.getParent();
				if(!sourcefolder.endsWith('sites'))
					{
												
						 String metaSource = source+META_DATA_SUFIX
						 String metaTarget = target+META_DATA_SUFIX
						 println metaSource
						 println metaTarget
				         (new AntBuilder()).copy(file: metaSource, tofile: metaTarget)
											
				         copyParentMetaData(metaSource)
         		 }
        }
                                
        if(hasLightningMetaData(source)){
									
              def sourcefile = new File(source)
                println sourcefile.getParent() 
                def sourcefolder = sourcefile.getParent();
                if(!sourcefolder.endsWith('applications'))
                   {
					 println "sourcefolder"
                     println sourcefolder
					 def targetfile = new File(target)
                     println targetfile.getParent()
                     def targetfolder = targetfile.getParent();
                      println "targetfolder"
                      println targetfolder
                      new AntBuilder().copy( todir: targetfolder ) {
                      fileset( dir: sourcefolder ) }
                    }
                                              
          }


      }
    }
  }

  /**
   * Get the sha id from the version.txt on the sfdc-service repository,
   * the sha id can be used to retrieve the list of modified files
   */
  String getVersion(){
    def versionPath = "${sourceDir}${FILE_SEPARATOR}${VERSION_FILE_NAME}"
    def versionFile = new File(versionPath)
    versionFile.text
  }

  /**
   * Get a list of list of modified files since an specific version
   */
  String[] getModifiedFiles(String version){
    def command = "git diff --oneline --name-status ${version} HEAD"
    println "before run command"
    def output = runCommand(command)

    println "after run command"
    def modifiedFiles = []

    output.split('\n').each { line ->
      // Remove the first charactare because it only indicates the file status
      def modifiedFile = line.substring(1).trim()

      modifiedFiles << modifiedFile
    }

    return modifiedFiles
  }

  /**
   * Run a command in the operation system command prompt
   * return the exit of the command
   */
  String runCommand(String command){
    try
    {
      def proc = command.execute(null, new File(sourceDir))
      // any error message?
      StreamGobbler errorGobbler = new
        StreamGobbler(proc.getErrorStream(), "ERROR")

      // any output?
      StreamGobbler outputGobbler = new
        StreamGobbler(proc.getInputStream(), "OUTPUT")

      // kick them off
      errorGobbler.start();
      outputGobbler.start();

      // any error???
      int exitVal = proc.waitFor();
      errorGobbler.join();
      outputGobbler.join();
      def exit = outputGobbler.buffer
      println exit
      return exit
    } catch (Throwable t)
    {
      t.printStackTrace();
      return "Error"
    }
  }

  /**
   * Return true if the provided file is on the src folder and false in other way
   */
  boolean isOnSrc ( String fileName){
    fileName.startsWith("src")
  }

  /**
   * Return true if the file provided have one of the suffix listed on
   * META_DATA_SUFIX, it helps to determine if a file have meta data or not
   */
  boolean hasMetaData(String fileName){
    boolean hasMetaData = false

    for ( sufix in META_DATA_NEEDED){
      hasMetaData |= fileName.endsWith(sufix)
    }

    return hasMetaData
  }
  
  /**
   * Return true if the file provided have one of the suffix listed on
   * LIGHTNING_COMPONENTS, it helps to determine if a file belongs to aura bundle or not
   */
  boolean hasLightningMetaData(String fileName){
    boolean hasLightningMetaData = false

    for ( sufix in LIGHTNING_COMPONENTS){
      hasLightningMetaData |= fileName.endsWith(sufix)
    }

    return hasLightningMetaData
  }

  /**
   * Create distribution folder for a destructive changes file
   */
  def createDestructivePkg(){
      String destructivePath = "${baseDir}/${DISTRIBUTION_FOLDER_DESTRUCTIVE}/"
      String destructiveFile = "destructiveChanges.xml"
      String destructivePkgPath = "${destructivePath}package.xml"
      File destructivePackage = new File(destructivePkgPath)
      destructivePackage.withWriter { out ->
        EMPTY_PACKAGE_XML.split('\n').each() { line ->
          out.writeLine(line)
        }
      }
      String source = "${sourceDir}/src/${destructiveFile}"
      String target = "${destructivePath}${destructiveFile}"
      println destructiveFile
      println target
      (new AntBuilder()).copy(file: source, tofile: target)
  }

  /**
   * Create a folder structure necesary for a file
   */
  def createParentDir(String filePath){
    def file = new File(filePath)
    def parent = file.getParentFile()

    if(!parent.exists()){
      parent.mkdirs()
    }

  }


  /**
   * Some folders also have meta data, this method
   * try to copy folder meta data if exist
   */
  def copyParentMetaData(aFile){

    def file = new File(aFile)
    def parent = file.getParent()
    def metaDataName = parent  + META_DATA_SUFIX
    def metaData = new File(metaDataName)
    def target = metaDataName.
      replace(
      "${sourceDir}${FILE_SEPARATOR}",
      "${destinationDir}${FILE_SEPARATOR}")

    if(metaData.exists()){
        (new AntBuilder()).copy(file: metaDataName, tofile: target)
    }
  }

  /**
   * The worflows can contians real emails that referes to target.com,
   * this methed modify all the files on a directory, to replace
   * every occurence of target.com to tet.target.com
   */
  static def addFakeEmailWorkflows(String workflowDirPath){
          File dir = new File(workflowDirPath)

          dir.eachFileRecurse (FileType.FILES) { workflow ->
             String content = workflow.text
             PrintWriter writer = new PrintWriter(workflow)
             writer.print content.replaceAll('@[tT]arget.com</ccEmails>', '@test.target.com</ccEmails>')
             writer.close()
          }
  }

  /**
   * Helper class to control the output of the commands
   */
  class StreamGobbler extends Thread
  {
    InputStream is;
    String type;
    String buffer = ""

    StreamGobbler(InputStream is, String type)
    {
      this.is = is;
      this.type = type;
    }

    public void run()
    {
      try
      {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line=null;
        while ( (line = br.readLine()) != null)
          buffer += line+"\n"
      } catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
    }
  }
}
