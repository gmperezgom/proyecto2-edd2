package librosdigitales;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

// Pruebas reproducibles sin dependencias externas. Todos los archivos se crean en una carpeta de prueba nueva.
public final class PruebasIntegracion {
    private static int verificaciones;
    private static Path raiz;
    private static final String TEXTO="Capitulo 1\nHabia una vez una ceiba enorme en medio de la plaza.\nTodos los ninos del pueblo jugaban bajo su sombra.\n";
    private interface Operacion { void ejecutar() throws Exception; }
    private static void comprobar(boolean condicion,String nombre) {
        if(!condicion) throw new AssertionError(nombre);
        verificaciones++;
    }
    private static void rechaza(Operacion operacion,String nombre) throws Exception {
        try { operacion.ejecutar(); } catch(IOException | IllegalArgumentException e) { verificaciones++; return; }
        throw new AssertionError("No rechazó: "+nombre);
    }
    private static void grupo(String nombre) { System.out.println("PRUEBA: "+nombre); }

    public static void main(String[] args) throws Exception {
        Path base=Path.of(args.length>0?args[0]:"work/pruebas"); Files.createDirectories(base);
        raiz=Files.createTempDirectory(base,"libros-v2-").toAbsolutePath();
        algoritmos();
        System.out.println("ALGORITMOS_OK="+verificaciones);
        if(args.length>1 && args[1].equals("--solo-local")) { localesSinRed(); return; }
        int puerto;
        try(ServerSocket libre=new ServerSocket(0)) { puerto=libre.getLocalPort(); }
        Path datos=raiz.resolve("servidor_v2");
        Path configServidor=raiz.resolve("servidor-config-v2.txt");
        GeneradorServidor.generar(datos.toString(),configServidor.toString(),null,puerto);
        Configuracion config=Configuracion.cargar(raiz.resolve("config-cliente-v2.ejemplo.txt").toString());
        config.setRutaLocal(raiz.resolve("cliente").toString());
        String clienteConfig=ArchivoUtil.leerTexto(raiz.resolve("config-cliente-v2.ejemplo.txt").toString());
        clienteConfig=clienteConfig.replace("rutaLocal=cliente_v2","rutaLocal="+config.getRutaLocal());
        ArchivoUtil.escribirTexto(raiz.resolve("config.txt").toString(),clienteConfig);
        RepositorioLocal local=new RepositorioLocal(config.getRutaLocal());
        AutenticacionLocal auth=new AutenticacionLocal(local);
        ConexionServidor conexion=new ConexionServidor(config);
        Usuario ana;
        String hashAna=HashUtil.sha256("ana123");
        String hashLuis=HashUtil.sha256("luis123");
        grupo("Servidor real TCP, identidad, autenticación, permisos y concurrencia");
        ServidorLibros servidor=new ServidorLibros(configServidor.toString());
        Thread hilo=new Thread(()->{try {servidor.servir();} catch(IOException e) {throw new RuntimeException(e);}});
        hilo.start();
        try {
            comprobar(conexion.consultarCatalogo().getTamano()==3,"catálogo de red");
            ana=conexion.autenticar("ana",hashAna);
            Usuario luis=conexion.autenticar("luis",hashLuis);
            comprobar(ana.isPremium() && !luis.isPremium(),"categorías obtenidas del servidor");
            rechaza(()->conexion.autenticar("ana",HashUtil.sha256("incorrecta")),"contraseña incorrecta");
            Libro libro=conexion.consultarCatalogo().obtener(0);
            String texto=conexion.obtenerLibro(ana,libro,true,local);
            comprobar(texto.equals(TEXTO),"lectura con secreto negociado");
            String original=ArchivoUtil.leerTexto(datos.resolve("libros").resolve(libro.getIsbn()+".txt").toString());
            String copia=ArchivoUtil.leerTexto(local.carpetaUsuario("ana").resolve("copias_cifradas").resolve(libro.getIsbn()+".txt").toString());
            comprobar(original.equals(copia),"copia cifrada idéntica al archivo del servidor");
            Path entrega=local.carpetaUsuario("ana").resolve("entregas_cifradas").resolve(libro.getIsbn()+".txt");
            comprobar(Files.isRegularFile(entrega) && !ArchivoUtil.leerTexto(entrega.toString()).equals(original),"entrega independiente del cifrado de almacenamiento");
            local.guardarDescarga("ana",libro.getIsbn(),texto);
            comprobar(local.leerDescarga("ana",libro.getIsbn()).equals(TEXTO),"descarga y descompresión");
            rechaza(()->conexion.obtenerLibro(luis,libro,true,local),"descarga estándar rechazada por el servidor");
            rechaza(()->conexion.obtenerLibro(ana,new Libro("9999999999999","Falso"),false,local),"ISBN fuera del catálogo");
            ExecutorService clientes=Executors.newFixedThreadPool(4);
            try {
                java.util.List<Future<String>> respuestas=new ArrayList<>();
                for(int i=0;i<8;i++) {
                    final Usuario usuario=i%2==0?ana:luis;
                    respuestas.add(clientes.submit(()->new ConexionServidor(config).obtenerLibro(usuario,libro,false,local)));
                }
                for(Future<String> r:respuestas) comprobar(r.get(20,TimeUnit.SECONDS).equals(TEXTO),"lectura concurrente");
            } finally {clientes.shutdownNow();}
            String malaHuella=clienteConfig.replace(config.getHuellaServidor(),"0".repeat(64));
            Path falso=raiz.resolve("config-huella-incorrecta.txt"); ArchivoUtil.escribirTexto(falso.toString(),malaHuella);
            rechaza(()->new ConexionServidor(Configuracion.cargar(falso.toString())).autenticar("ana",hashAna),"identidad del servidor diferente");
            // Un mensaje de longitud negativa no debe finalizar el servicio.
            try(Socket malicioso=new Socket("localhost",puerto)) {
                malicioso.setSoTimeout(5000);
                new DataOutputStream(malicioso.getOutputStream()).writeInt(-1);
                comprobar(malicioso.getInputStream().read()==-1,"longitud de red negativa rechazada");
            }
            comprobar(conexion.consultarCatalogo().getTamano()==3,"servidor sigue activo tras mensaje inválido");
            Path archivoLibro=datos.resolve("libros").resolve(libro.getIsbn()+".txt");
            ArchivoUtil.escribirTexto(archivoLibro.toString(),alterar(original));
            rechaza(()->conexion.obtenerLibro(ana,libro,false,new RepositorioLocal(raiz.resolve("cliente-rechazado").toString())),"libro del almacén alterado");
            ArchivoUtil.escribirTexto(archivoLibro.toString(),original);
            auth.preparar(ana,"ana123".toCharArray(),conexion.consultarCatalogo());
            comprobar(auth.autenticar("ana","ana123".toCharArray()).getUsuario().isPremium(),"cuenta local Premium preparada");
            rechaza(()->auth.autenticar("ana","mala".toCharArray()),"contraseña local incorrecta");
            auth.preparar(luis,"luis123".toCharArray(),conexion.consultarCatalogo());
            rechaza(()->auth.autenticar("luis","luis123".toCharArray()),"estándar sin acceso local");
            comprobar(local.listarDescargas("luis").estaVacia(),"descargas separadas por usuario");
        } finally {servidor.close(); hilo.join(15000);}
        grupo("Acceso local con servicio detenido");
        rechaza(()->conexion.consultarCatalogo(),"servicio detenido");
        comprobar(auth.autenticar("ana","ana123".toCharArray()).getCatalogo().getTamano()==3,"autenticación sin red");
        comprobar(local.leerDescarga("ana","9780000000001").equals(TEXTO),"lectura sin red");
        String cuenta=ArchivoUtil.leerTexto(local.rutaCuenta("ana").toString());
        comprobar(!cuenta.contains("ana123") && !cuenta.contains(hashAna),"sin contraseña ni hash de acceso en el archivo local");
        ArchivoUtil.escribirTexto(local.rutaCuenta("ana").toString(),alterar(cuenta));
        rechaza(()->auth.autenticar("ana","ana123".toCharArray()),"cuenta local alterada");
        ArchivoUtil.escribirTexto(local.rutaCuenta("ana").toString(),cuenta);
        auth.preparar(new Usuario("ana",hashAna,false),"ana123".toCharArray(),new ListaEnlazada<>());
        rechaza(()->auth.autenticar("ana","ana123".toCharArray()),"revocación local al validar categoría estándar en línea");
        auth.preparar(new Usuario("ana",hashAna,true),"ana123".toCharArray(),RepositorioServidor.analizarCatalogo("9780000000001|El viaje de la ceiba\n"));
        grupo("Archivos inválidos y protección de datos existentes");
        rechaza(()->RepositorioServidor.analizarCatalogo("../afuera|Título"),"ruta maliciosa");
        rechaza(()->RepositorioServidor.analizarCatalogo("9780000000001|A\n9780000000001|B"),"ISBN duplicado");
        Path usuarios=datos.resolve("usuarios.txt"); String usuariosOriginal=ArchivoUtil.leerTexto(usuarios.toString());
        ArchivoUtil.escribirTexto(usuarios.toString(),"ana|hash-invalido|quizas");
        rechaza(()->new RepositorioServidor(datos.toString()).cargarUsuarios(),"registro de usuario inválido");
        ArchivoUtil.escribirTexto(usuarios.toString(),usuariosOriginal);
        rechaza(()->GeneradorServidor.generar(datos.toString(),configServidor.toString(),null,puerto),"generador no sobrescribe");
        comprobar(ArchivoUtil.leerTexto(usuarios.toString()).equals(usuariosOriginal),"datos existentes conservados");
        Path utf=raiz.resolve("utf-invalido.txt"); Files.write(utf,new byte[]{(byte)0xc3,0x28});
        rechaza(()->ArchivoUtil.leerTexto(utf.toString()),"UTF-8 inválido");
        String configuracion=ArchivoUtil.leerTexto(raiz.resolve("config.txt").toString());
        Path duplicada=raiz.resolve("duplicada.txt"); ArchivoUtil.escribirTexto(duplicada.toString(),configuracion+"hostServidor=otro\n");
        rechaza(()->Configuracion.cargar(duplicada.toString()),"configuración duplicada");
        if(args.length>1) {
            grupo("Migración real de los archivos V1 sin modificar el origen");
            Path destino=raiz.resolve("migracion"); Files.createDirectories(destino);
            Path origen=Path.of(args[1]);
            String antes=ArchivoUtil.leerTexto(origen.resolve("libros/9780000000001.txt").toString());
            GeneradorServidor.generar(destino.resolve("datos").toString(),destino.resolve("servidor-config-v2.txt").toString(),origen.toString(),puerto);
            String cfg=ArchivoUtil.leerTexto(destino.resolve("servidor-config-v2.txt").toString());
            byte[] clave=Base64.getDecoder().decode(Configuracion.valor(cfg,"claveAlmacen",null));
            String migrado=new RepositorioServidor(destino.resolve("datos").toString()).leerLibroCifrado("9780000000001");
            comprobar(new Cifrador3DES(clave).descifrar(migrado).equals(TEXTO),"contenido migrado");
            comprobar(antes.equals(ArchivoUtil.leerTexto(origen.resolve("libros/9780000000001.txt").toString())),"origen V1 intacto");
        }
        System.out.println("RESULTADO: "+verificaciones+" verificaciones correctas.");
        System.out.println("DIRECTORIO_PRUEBAS="+raiz);
    }

    private static void algoritmos() throws Exception {
        grupo("Hash, DH independiente, 3DES autenticado y Huffman");
        comprobar(HashUtil.sha256("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),"vector SHA-256");
        comprobar(DiffieHellman.P.bitLength()==2048 && DiffieHellman.P.isProbablePrime(64),"grupo DH de 2048 bits");
        BigInteger a=DiffieHellman.generarPrivada(), b=DiffieHellman.generarPrivada();
        BigInteger A=DiffieHellman.calcularPublica(a), B=DiffieHellman.calcularPublica(b);
        BigInteger s=DiffieHellman.calcularSecreto(B,a);
        comprobar(s.equals(DiffieHellman.calcularSecreto(A,b)),"secretos iguales en participantes diferentes");
        comprobar(!A.equals(B),"públicas independientes");
        rechaza(()->DiffieHellman.validarPublica(BigInteger.ONE),"pública DH inválida");
        Cifrador3DES cif=new Cifrador3DES(s);
        String[] casos={"","aaaaaa","abcabc", "Español: áéíóú\n📚", "\uFEFFTexto con BOM inicial", "a".repeat(100000),"a\nb\r\nc|d"};
        for(String c:casos) {
            comprobar(c.equals(Huffman.descomprimir(Huffman.comprimir(c))),"Huffman casos de borde");
            comprobar(c.equals(cif.descifrar(cif.cifrar(c))),"3DES casos de borde");
        }
        comprobar(!cif.cifrar("igual").equals(cif.cifrar("igual")),"IV aleatorio");
        rechaza(()->cif.descifrar(alterar(cif.cifrar("texto"))),"3DES dañado");
        rechaza(()->new Cifrador3DES(HashUtil.aleatorios(32)).descifrar(cif.cifrar("texto")),"clave 3DES errónea");
        Random azar=new Random(8127);
        for(int k=0;k<30;k++) {
            StringBuilder s1=new StringBuilder();
            for(int i=0;i<500+k*17;i++) s1.append((char)(' '+azar.nextInt(90)));
            String texto=s1.toString();
            comprobar(texto.equals(Huffman.descomprimir(Huffman.comprimir(texto))),"Huffman aleatorio reproducible");
        }
        String comprimido=Huffman.comprimir("abracadabra y caracteres áéíóú\n".repeat(20));
        // Todos los prefijos truncados deben fallar.
        for(int i=0;i<comprimido.length();i++) {
            final String incompleto=comprimido.substring(0,i);
            rechaza(()->Huffman.descomprimir(incompleto),"Huffman truncado en "+i);
        }
        String[] lineas=comprimido.split("\n",-1);
        for(int i=0;i<6;i++) {
            String[] rotas=lineas.clone(); rotas[i]=i==3?"97:0":i==4?"@@@":i==5?"0".repeat(64):"-1";
            final String archivo=String.join("\n",rotas);
            rechaza(()->Huffman.descomprimir(archivo),"metadato Huffman inválido "+i);
        }
        String[] rotas=lineas.clone(); rotas[4]=alterar(rotas[4]);
        rechaza(()->Huffman.descomprimir(String.join("\n",rotas)),"bits alterados");
        String[] unicos=Huffman.comprimir("aaa").split("\n",-1); unicos[4]="gA==";
        rechaza(()->Huffman.descomprimir(String.join("\n",unicos)),"bits de un solo carácter alterados");
        rechaza(()->Huffman.descomprimir("HUFFMAN\n1\n97:1\nAA=="),"formato antiguo sin hash");
    }

    // Permite comprobar caché y arranque sin conexión incluso en un entorno sin permiso de sockets.
    private static void localesSinRed() throws Exception {
        grupo("Autenticación y lectura locales sin abrir sockets");
        RepositorioLocal local=new RepositorioLocal(raiz.resolve("cliente").toString());
        AutenticacionLocal auth=new AutenticacionLocal(local);
        Usuario ana=new Usuario("ana",HashUtil.sha256("ana123"),true);
        Usuario luis=new Usuario("luis",HashUtil.sha256("luis123"),false);
        ListaEnlazada<Libro> libros=RepositorioServidor.analizarCatalogo("9780000000001|El viaje de la ceiba\n");
        auth.preparar(ana,"ana123".toCharArray(),libros);
        comprobar(auth.autenticar("ana","ana123".toCharArray()).getUsuario().isPremium(),"Premium local válido");
        rechaza(()->auth.autenticar("ana","mala".toCharArray()),"contraseña local incorrecta");
        auth.preparar(luis,"luis123".toCharArray(),libros);
        rechaza(()->auth.autenticar("luis","luis123".toCharArray()),"estándar sin caché");
        local.guardarDescarga("ana","9780000000001",TEXTO);
        comprobar(local.leerDescarga("ana","9780000000001").equals(TEXTO),"lectura local");
        comprobar(local.listarDescargas("luis").estaVacia(),"separación por usuario");
        String cuenta=ArchivoUtil.leerTexto(local.rutaCuenta("ana").toString());
        ArchivoUtil.escribirTexto(local.rutaCuenta("ana").toString(),alterar(cuenta));
        rechaza(()->auth.autenticar("ana","ana123".toCharArray()),"cuenta alterada");
        ArchivoUtil.escribirTexto(local.rutaCuenta("ana").toString(),cuenta);
        auth.preparar(new Usuario("ana",HashUtil.sha256("ana123"),false),"ana123".toCharArray(),libros);
        rechaza(()->auth.autenticar("ana","ana123".toCharArray()),"revocación de categoría");
        auth.preparar(ana,"ana123".toCharArray(),libros);
        String config="rutaServidor=referencia\nrutaLocal="+raiz.resolve("cliente")+"\nhostServidor=localhost\npuertoServidor=5050\nhuellaServidor="+"0".repeat(64)+"\n";
        ArchivoUtil.escribirTexto(raiz.resolve("config.txt").toString(),config);
        System.out.println("RESULTADO_LOCAL: "+verificaciones+" verificaciones correctas.");
        System.out.println("DIRECTORIO_PRUEBAS="+raiz);
    }

    private static String alterar(String texto) {
        int i=texto.length()-1;
        return texto.substring(0,i)+(texto.charAt(i)=='A'?'B':'A');
    }
}
