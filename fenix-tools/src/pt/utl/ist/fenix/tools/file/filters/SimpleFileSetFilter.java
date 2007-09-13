package pt.utl.ist.fenix.tools.file.filters;

import java.io.File;
import java.util.Collection;

import javax.activation.MimetypesFileTypeMap;

import pt.utl.ist.fenix.tools.file.FileSet;
import pt.utl.ist.fenix.tools.file.FileSetMetaData;

public class SimpleFileSetFilter extends RecursiveFileSetFilter {

	public SimpleFileSetFilter() {
		super();
	}

	@Override
	public void handleFileSetLevel(FileSet leveledFs) throws FileSetFilterException {
		
		Collection<File> supposedFiles=leveledFs.getContentFiles();
		if(supposedFiles!=null && supposedFiles.size()!=0)
		{
			File supposedFile=supposedFiles.toArray(new File[0])[0];
			if(supposedFile.exists() && supposedFile.canRead())
			{
				String findByFileName=MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(supposedFile.getName());
				String mimeType = (findByFileName==null) ? "application/octet-stream" :findByFileName; 
				leveledFs.addMetaInfo(new FileSetMetaData("format","extent",null,""+supposedFile.length()));
				leveledFs.addMetaInfo(new FileSetMetaData("format","mimetype",null,mimeType));
			}
		}
	}

}
