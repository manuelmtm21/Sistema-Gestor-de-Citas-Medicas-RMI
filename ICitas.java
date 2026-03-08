package Servidor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ICitas extends Remote {
    // Autenticación
    boolean login(String usuario, String contraseña, String tipoUsuario) throws RemoteException;
    boolean registrarUsuario(String usuario, String contraseña, String tipo) throws RemoteException;
    boolean modificarUsuario(String usuarioActual, String nuevoUsuario, String nuevaContraseña, String nuevoTipo) throws RemoteException;
    boolean eliminarUsuario(String usuario) throws RemoteException;
    
    // Funciones comunes
    List<String> buscarCitas(String filtro) throws RemoteException;
    boolean citaDisponible(String doctor, String fecha, String hora) throws RemoteException;
    List<String> obtenerUsuariosPorTipo(String tipo) throws RemoteException;
    
    // Funciones para pacientes
    boolean solicitarCita(String paciente, String doctor, String fecha, String hora, String asunto) throws RemoteException;
    List<String> verCitasPaciente(String paciente) throws RemoteException;
    
    // Funciones para doctores
    boolean agendarCita(String doctor, String paciente, String fecha, String hora, String asunto) throws RemoteException;
    boolean modificarCita(int idCita, String nuevoPaciente, String nuevaFecha, String nuevaHora, String nuevoAsunto) throws RemoteException;
    boolean cancelarCita(int idCita) throws RemoteException;
    boolean eliminarCita(int id) throws RemoteException;
    boolean reactivarCita(int id) throws RemoteException;
    boolean existeDoctor(String nombreDoctor) throws RemoteException;
    List<String> verCitasDoctor(String doctor) throws RemoteException;
}
