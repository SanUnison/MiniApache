import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


//Se procesan los directorios y archivos en la ruta, agregando a listas separadas.
public class ExploraRuta extends SimpleFileVisitor<Path> {

    LinkedList<String> fileNameList = new LinkedList<>();
    LinkedList<Path> dirNameList = new LinkedList<>();
    Path ruta;


    ExploraRuta(Path path) {
        this.ruta = path;
        dirNameList.add(Paths.get("/.."));
    }

    public LinkedList<String> getFileNameList() {
        return fileNameList;
    }

    public LinkedList<Path> getDirNameList() {
        return dirNameList;
    }

    //Por cada directorio visitado
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        String r = dir.toString().replaceAll(ruta.toString(),"");
        String[] rutaDirectorio = r.split("/");

        System.out.println("Arrays.toString(rutDo) = " + Arrays.toString(rutaDirectorio));

        if (rutaDirectorio.length > 1 && !dirNameList.contains(Paths.get("/" + rutaDirectorio[1]))){
            dirNameList.add(Paths.get("/" + rutaDirectorio[1]));
        }
            return super.preVisitDirectory(dir, attrs);
    }
    //Por cada archivo visitado
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        FileReader fi;
        BufferedReader in;
        //String name = file.toAbsolutePath().toString();
        String fileName = file.getFileName().toString();

        if (file.getParent().getFileName().equals(ruta.getFileName())){
            System.out.println("fileName = " + fileName);
            fileNameList.add(fileName);
        }

        return super.visitFile(file, attrs);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        System.out.printf("No se puede procesar el archivo %20s", file.toString());
        return super.visitFileFailed(file, exc);
    }
}
