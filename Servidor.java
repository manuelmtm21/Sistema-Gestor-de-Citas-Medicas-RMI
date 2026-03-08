package Servidor;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.*;

public class Servidor {
    public static void main(String[] args) {
        try {
            String serverIP = InetAddress.getLocalHost().getHostAddress();
            System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress());
            
            GestorCitas gestor = new GestorCitas();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("CitaService", gestor);
            
            if (gestor.getUsuarios().isEmpty()) {
                // Esto se deja de ejemplo de los usuarios pre-registrados...
                // Usuario(s) administrador por defecto
                gestor.registrarUsuario("admin", "admin123", "Administrador");
                
                // Doctores
                gestor.registrarUsuario("Dr. Omar", "doc123", "Doctor");
                gestor.registrarUsuario("Dr. Carmona", "doc123", "Doctor");
                gestor.registrarUsuario("Dra. Georgina", "doc123", "Doctor");
                gestor.registrarUsuario("Dra. Josefina", "doc123", "Doctor");
                
                // Pacientes
                gestor.registrarUsuario("Manuel", "123", "Paciente");
                gestor.registrarUsuario("Francisco", "123", "Paciente");
                gestor.registrarUsuario("Gustavo", "123", "Paciente");
                gestor.registrarUsuario("Gabriel", "123", "Paciente");
                gestor.registrarUsuario("Brenda", "123", "Paciente");
                gestor.registrarUsuario("Yahir", "123", "Paciente");
                gestor.registrarUsuario("David", "123", "Paciente");
                gestor.guardarDatos();
            }
            
            System.out.println("Servidor RMI listo en la IP: " + serverIP);
            System.out.println("Usa esta IP en el cliente para conectarte");
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Guardando datos antes de cerrar...");
                gestor.guardarDatos();
            }));
            
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class GestorCitas extends UnicastRemoteObject implements ICitas {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "citas.dat";
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Cita> citas = new ArrayList<>();
    
    public GestorCitas() throws RemoteException {
        super();
        cargarDatos();
    }
    
    public synchronized List<Usuario> getUsuarios() {
        return new ArrayList<>(usuarios);
    }

    public synchronized void guardarDatos() {
        try {
            Path tempFile = Files.createTempFile("citas", ".tmp");
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(tempFile))) {
                oos.writeObject(usuarios);
                oos.writeObject(citas);
            }
            
            Files.move(tempFile, Path.of(DATA_FILE), 
                      StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);
            
            System.out.println("Datos guardados correctamente");
        } catch (IOException e) {
            System.err.println("Error guardando datos: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void cargarDatos() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(DATA_FILE))) {
                    
                    usuarios = (List<Usuario>) ois.readObject();
                    citas = (List<Cita>) ois.readObject();
                    
                    if (!citas.isEmpty()) {
                        Cita.contador = citas.stream()
                            .mapToInt(Cita::getId)
                            .max()
                            .orElse(0) + 1;
                    }
                    
                    System.out.println("Datos cargados: " + usuarios.size() + 
                                     " usuarios, " + citas.size() + " citas");
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando datos: " + e.getMessage());
            usuarios = new ArrayList<>();
            citas = new ArrayList<>();
        }
    }

    @Override
    public boolean login(String usuario, String contraseña, String tipo) throws RemoteException {
        return usuarios.stream()
            .anyMatch(u -> u.validarCredenciales(usuario, contraseña) && u.getTipo().equalsIgnoreCase(tipo));
    }
    
    @Override
    public boolean registrarUsuario(String usuario, String contraseña, String tipo) throws RemoteException {
        if (usuarios.stream().anyMatch(u -> u.getUsuario().equalsIgnoreCase(usuario))) {
            return false;
        }
        usuarios.add(new Usuario(usuario, contraseña, tipo));
        guardarDatos();
        return true;
    }
    
    @Override
    public boolean modificarUsuario(String usuarioActual, String nuevoUsuario, String nuevaContraseña, String nuevoTipo) throws RemoteException {
        for (Usuario usuario : usuarios) {
            if (usuario.getUsuario().equalsIgnoreCase(usuarioActual)) {
                if (!nuevoUsuario.trim().isEmpty()) {
                    if (!nuevoUsuario.equalsIgnoreCase(usuarioActual) && 
                        usuarios.stream().anyMatch(u -> u.getUsuario().equalsIgnoreCase(nuevoUsuario))) {
                        return false;
                    }
                    usuario.usuario = nuevoUsuario;
                }
                if (!nuevaContraseña.trim().isEmpty()) {
                    usuario.contraseña = nuevaContraseña;
                }
                if (!nuevoTipo.trim().isEmpty()) {
                    usuario.tipo = nuevoTipo;
                }
                guardarDatos();
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean eliminarUsuario(String usuario) throws RemoteException {
        boolean tieneCitas = citas.stream()
            .anyMatch(c -> (c.getPaciente().equalsIgnoreCase(usuario) || 
                          c.getDoctor().equalsIgnoreCase(usuario)) &&
                          !c.getEstado().equalsIgnoreCase("cancelada"));
        
        if (tieneCitas) return false;
        
        Iterator<Usuario> iterator = usuarios.iterator();
        while (iterator.hasNext()) {
            Usuario u = iterator.next();
            if (u.getUsuario().equalsIgnoreCase(usuario)) {
                iterator.remove();
                
                citas.removeIf(c -> (c.getPaciente().equalsIgnoreCase(usuario) || 
                                   c.getDoctor().equalsIgnoreCase(usuario)));
                
                guardarDatos();
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<String> obtenerUsuariosPorTipo(String tipo) throws RemoteException {
        List<String> usuariosFiltrados = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            if (usuario.getTipo().equalsIgnoreCase(tipo)) {
                usuariosFiltrados.add(usuario.getUsuario());
            }
        }
        return usuariosFiltrados;
    }
    
    @Override
    public boolean solicitarCita(String paciente, String doctor, String fecha, String hora, String asunto) throws RemoteException {
        if (!citaDisponible(doctor, fecha, hora)) return false;
        citas.add(new Cita(paciente, doctor, fecha, hora, asunto));
        guardarDatos();
        return true;
    }
    
    @Override
    public boolean agendarCita(String doctor, String paciente, String fecha, String hora, String asunto) throws RemoteException {
        citas.add(new Cita(paciente, doctor, fecha, hora, asunto));
        guardarDatos();
        return true;
    }
    
    @Override
    public boolean modificarCita(int id, String nuevoPaciente, String nuevaFecha, String nuevaHora, String nuevoAsunto) throws RemoteException {
        for (Cita cita : citas) {
            if (cita.getId() == id) {
                if (nuevoPaciente != null && !nuevoPaciente.trim().isEmpty()) {
                    cita.setPaciente(nuevoPaciente.trim());
                }
                cita.setFechaHora(nuevaFecha, nuevaHora);
                if (nuevoAsunto != null && !nuevoAsunto.trim().isEmpty()) {
                    cita.setAsunto(nuevoAsunto.trim());
                }
                guardarDatos();
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean cancelarCita(int id) throws RemoteException {
        for (Cita cita : citas) {
            if (cita.getId() == id) {
                cita.setEstado("cancelada");
                guardarDatos();
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean eliminarCita(int id) throws RemoteException {
        Iterator<Cita> iterator = citas.iterator();
        while (iterator.hasNext()) {
            Cita c = iterator.next();
            if (c.getId() == id && c.getEstado().equalsIgnoreCase("cancelada")) {
                iterator.remove();
                guardarDatos();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean existeDoctor(String nombreDoctor) throws RemoteException {
        return usuarios.stream()
            .anyMatch(u -> u.getTipo().equalsIgnoreCase("Doctor") && 
                          u.getUsuario().equalsIgnoreCase(nombreDoctor));
    }

    @Override
    public boolean reactivarCita(int id) throws RemoteException {
        for (Cita cita : citas) {
            if (cita.getId() == id && cita.getEstado().equalsIgnoreCase("cancelada")) {
                if (!citaDisponible(cita.getDoctor(), cita.getFecha(), cita.getHora())) {
                    return false;
                }
                cita.setEstado("pendiente");
                guardarDatos();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean citaDisponible(String doctor, String fecha, String hora) throws RemoteException {
        return citas.stream()
            .noneMatch(c -> c.getDoctor().equalsIgnoreCase(doctor) && 
                          c.getFecha().equalsIgnoreCase(fecha) && 
                          c.getHora().equalsIgnoreCase(hora) &&
                          !c.getEstado().equalsIgnoreCase("cancelada"));
    }

    @Override
    public List<String> buscarCitas(String filtro) throws RemoteException {
        List<String> resultados = new ArrayList<>();
        citas.stream()
            .filter(c -> c.toString().toLowerCase().contains(filtro.toLowerCase()))
            .forEach(c -> resultados.add(c.toString()));
        return resultados;
    }
    
    @Override
    public List<String> verCitasPaciente(String paciente) throws RemoteException {
        List<String> resultados = new ArrayList<>();
        citas.stream()
            .filter(c -> c.getPaciente().equalsIgnoreCase(paciente))
            .forEach(c -> resultados.add(c.toString()));
        return resultados;
    }
    
    @Override
    public List<String> verCitasDoctor(String doctor) throws RemoteException {
        List<String> resultados = new ArrayList<>();
        citas.stream()
            .filter(c -> c.getDoctor().equalsIgnoreCase(doctor))
            .forEach(c -> resultados.add(c.toString()));
        return resultados;
    }
}

class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    String usuario;
    String contraseña;
    String tipo;
    
    public Usuario(String usuario, String contraseña, String tipo) {
        this.usuario = usuario;
        this.contraseña = contraseña;
        this.tipo = tipo;
    }
    
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public boolean validarCredenciales(String usuario, String contraseña) {
        return this.usuario.equals(usuario) && this.contraseña.equals(contraseña);
    }
    
    public String getTipo() { return tipo; }
    public String getUsuario() { return usuario; }
}

class Cita implements Serializable {
    private static final long serialVersionUID = 1L;
    public static int contador = 1;
    
    private int id;
    private String paciente, doctor, fecha, hora, estado, asunto;
    
    public Cita(String paciente, String doctor, String fecha, String hora, String asunto) {
        this.id = contador++;
        this.paciente = paciente;
        this.doctor = doctor;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = "pendiente";
        this.asunto = asunto;
    }
    
    public int getId() { return id; }
    public String getPaciente() { return paciente; }
    public String getDoctor() { return doctor; }
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getEstado() { return estado; }
    public String getAsunto() { return asunto; }
    public void setPaciente(String paciente) { this.paciente = paciente; }
    public void setFechaHora(String fecha, String hora) { 
        this.fecha = fecha; 
        this.hora = hora; 
    }
    public void setEstado(String estado) { this.estado = estado; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    
    @Override
    public String toString() {
        return String.format("[%d] %s - Dr. %s | %s %s | Asunto: %s | Estado: %s", 
            id, paciente, doctor, fecha, hora, asunto, estado);
    }
}
