package grid.control;
import com.sun.net.httpserver.HttpServer;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.concurrent.CopyOnWriteArrayList;import lanzadores.LanzadorGrid;
/** Ejecutar en un directorio temporal vacío. No usa DBA Lab ni credenciales reales. */
public final class ArranqueSinArgumentosPrueba {
 public static void main(String[] args)throws Exception{
  if(Files.exists(Path.of("config.properties")))throw new IllegalStateException("Usar directorio temporal vacío");
  var eventos=new CopyOnWriteArrayList<String>();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/api/v1",e->{String ruta=e.getRequestURI().getPath();e.getRequestBody().readAllBytes();String json;
   if(ruta.endsWith("challenges"))json="{\"challenges\":[{\"id\":2,\"environmentType\":\"GRID_2D\",\"sensorCodes\":[\"grid.target\",\"grid.position\",\"grid.bumpers\",\"grid.energy\"],\"actionCodes\":[\"grid.move.north\",\"grid.move.east\",\"grid.move.south\",\"grid.move.west\"]}]}";
   else if(ruta.endsWith("/runs")){eventos.add("inicio");json="{\"control\":"+control("ACTIVE")+"}";}
   else if(ruta.endsWith("/actions")){eventos.add("accion");json="{\"actionStatus\":\"APPLIED\",\"control\":"+control("SUCCEEDED")+"}";}
   else {String valor=ruta.endsWith("grid.target")?"{\"x\":1,\"y\":0}":ruta.endsWith("grid.position")?"{\"x\":0,\"y\":0}":ruta.endsWith("grid.bumpers")?"{\"north\":true,\"east\":false,\"south\":true,\"west\":true}":"{\"mode\":\"FINITE\",\"remaining\":10}";json="{\"value\":"+valor+",\"control\":"+control("ACTIVE")+"}";}
   byte[] bytes=json.getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();
  });server.start();Files.writeString(Path.of("config.properties"),"DBA_API_URL=http://127.0.0.1:"+server.getAddress().getPort()+"/api/v1\nDBA_TOKEN=prueba-local\n");
  try(ServerSocket puerto=new ServerSocket(0)){System.setProperty("dba.jade.port",""+puerto.getLocalPort());}System.setProperty("dba.grid.ui","cli");
  jade.core.Runtime.instance().invokeOnTermination(()->{if(!eventos.equals(java.util.List.of("inicio","accion"))){System.err.println("Secuencia: "+eventos);System.exit(1);}System.out.println("ARRANQUE_SIN_ARGUMENTOS_OK");server.stop(0);});
  LanzadorGrid.main(new String[0]);
 }
 static String control(String s){return "{\"runId\":1,\"runStatus\":\""+s+"\",\"reasonCode\":null}";}
}
