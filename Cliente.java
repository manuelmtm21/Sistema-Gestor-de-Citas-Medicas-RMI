package Cliente;
import Servidor.ICitas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Cliente {
    public static ICitas obtenerStub(String serverIP) {
        try {
            String rmiURL = "rmi://" + serverIP + "/CitaService";
            System.out.println("Conectando a: " + rmiURL);
            return (ICitas) Naming.lookup(rmiURL);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error conectando al servidor: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog(
            null, 
            "Ingrese la IP del servidor:", 
            "Conectar al servidor", 
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (serverIP == null || serverIP.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Debe ingresar una IP válida");
            System.exit(0);
        }

        ICitas stub = obtenerStub(serverIP.trim());
        if (stub != null) {
            SwingUtilities.invokeLater(() -> new VentanaLogin(stub).setVisible(true));
        } else {
            System.exit(1);
        }
    }
}

class VentanaLogin extends JFrame {
    private ICitas stub;
    
    public VentanaLogin(ICitas stub) {
        this.stub = stub;
        setTitle("Login - Sistema de Citas");
        setSize(350, 200);
        setLayout(new GridLayout(4, 2, 10, 10));
        setLocationRelativeTo(null);
        
        JTextField txtUsuario = new JTextField();
        JPasswordField txtContraseña = new JPasswordField();
        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"Administrador", "Doctor", "Paciente"});
        JButton btnLogin = new JButton("Ingresar");
        
        add(new JLabel("Usuario:"));
        add(txtUsuario);
        add(new JLabel("Contraseña:"));
        add(txtContraseña);
        add(new JLabel("Tipo:"));
        add(cmbTipo);
        add(new JLabel());
        add(btnLogin);
        
        btnLogin.addActionListener(e -> {
            try {
                String usuario = txtUsuario.getText();
                String contraseña = new String(txtContraseña.getPassword());
                String tipo = (String) cmbTipo.getSelectedItem();
                
                if (stub.login(usuario, contraseña, tipo)) {
                    dispose();
                    new VentanaPrincipal(stub, tipo, usuario).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Credenciales incorrectas");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
    }
}

class VentanaPrincipal extends JFrame {
    private ICitas stub;
    private String tipoUsuario;
    private String usuario;

    public VentanaPrincipal(ICitas stub, String tipo, String usuario) {
        this.stub = stub;
        this.tipoUsuario = tipo;
        this.usuario = usuario;
        
        setTitle("Sistema de Citas - " + usuario + " (" + tipo + ")");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel panelUsuario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelUsuario.add(new JLabel("Usuario: " + usuario + " (" + tipo + ")"));
        panelSuperior.add(panelUsuario, BorderLayout.WEST);
        
        JButton btnSalir = new JButton("Cerrar Sesión");
        btnSalir.addActionListener(e -> {
            dispose();
            new VentanaLogin(stub).setVisible(true);
        });
        panelSuperior.add(btnSalir, BorderLayout.EAST);
        
        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);
        
        JTabbedPane pestañas = new JTabbedPane();
        
        if (tipo.equalsIgnoreCase("Administrador")) {
            pestañas.addTab("Administración", new PanelAdministracion(stub));
            pestañas.addTab("Gestión de Usuarios", new PanelGestionUsuarios(stub));
            pestañas.addTab("Gestión de Citas", new PanelDoctorCompleto(stub, usuario));
            pestañas.addTab("Buscar Citas", new PanelBuscarCitas(stub));
        } else if (tipo.equalsIgnoreCase("Doctor")) {
            pestañas.addTab("Gestión de Citas", new PanelDoctorCompleto(stub, usuario));
            pestañas.addTab("Buscar Citas", new PanelBuscarCitas(stub));
        } else {
            pestañas.addTab("Solicitar Cita", new PanelSolicitarCita(stub, usuario));
            pestañas.addTab("Mis Citas", new PanelCitasPaciente(stub, usuario));
            pestañas.addTab("Buscar Citas", new PanelBuscarCitas(stub));
        }
        
        panelPrincipal.add(pestañas, BorderLayout.CENTER);
        add(panelPrincipal);
    }
}

class PanelGestionUsuarios extends JPanel {
    private ICitas stub;
    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloLista;
    private JTextField txtUsuario, txtContraseña;
    private JComboBox<String> cmbTipo;
    
    public PanelGestionUsuarios(ICitas stub) {
        this.stub = stub;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        modeloLista = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloLista);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JPanel panelFormulario = new JPanel(new GridLayout(4, 2, 5, 5));
        txtUsuario = new JTextField();
        txtContraseña = new JPasswordField();
        cmbTipo = new JComboBox<>(new String[]{"", "Doctor", "Paciente", "Administrador"});
        
        panelFormulario.add(new JLabel("Usuario:"));
        panelFormulario.add(txtUsuario);
        panelFormulario.add(new JLabel("Contraseña:"));
        panelFormulario.add(txtContraseña);
        panelFormulario.add(new JLabel("Tipo:"));
        panelFormulario.add(cmbTipo);
        
        JButton btnCargar = new JButton("Cargar Selección");
        btnCargar.addActionListener(e -> cargarUsuarioSeleccionado());
        panelFormulario.add(btnCargar);
        
        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton btnModificar = new JButton("Modificar Usuario");
        btnModificar.setBackground(Color.BLUE);
        btnModificar.setForeground(Color.WHITE);
        btnModificar.addActionListener(e -> modificarUsuario());
        
        JButton btnEliminar = new JButton("Eliminar Usuario");
        btnEliminar.setBackground(Color.RED);
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.addActionListener(e -> eliminarUsuario());
        
        panelBotones.add(btnModificar);
        panelBotones.add(btnEliminar);
        
        JButton btnActualizar = new JButton("Actualizar Lista");
        btnActualizar.addActionListener(e -> actualizarListaUsuarios());
        
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(panelBotones, BorderLayout.CENTER);
        panelInferior.add(btnActualizar, BorderLayout.SOUTH);
        
        add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        add(panelFormulario, BorderLayout.NORTH);
        add(panelInferior, BorderLayout.SOUTH);
        
        actualizarListaUsuarios();
    }
    
    private void actualizarListaUsuarios() {
        try {
            modeloLista.clear();
            List<String> todosUsuarios = new ArrayList<>();
            todosUsuarios.addAll(stub.obtenerUsuariosPorTipo("Doctor"));
            todosUsuarios.addAll(stub.obtenerUsuariosPorTipo("Paciente"));
            todosUsuarios.addAll(stub.obtenerUsuariosPorTipo("Administrador"));
            
            if (todosUsuarios.isEmpty()) {
                modeloLista.addElement("No hay usuarios registrados");
            } else {
                todosUsuarios.forEach(modeloLista::addElement);
            }
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + ex.getMessage());
        }
    }
    
    private void cargarUsuarioSeleccionado() {
        String seleccion = listaUsuarios.getSelectedValue();
        if (seleccion != null && !seleccion.startsWith("No hay")) {
            txtUsuario.setText(seleccion);
            cmbTipo.setSelectedItem("");
            txtContraseña.setText("");
        }
    }
    
    private void modificarUsuario() {
        String usuarioActual = listaUsuarios.getSelectedValue();
        if (usuarioActual == null || usuarioActual.startsWith("No hay")) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario para modificar");
            return;
        }
        
        String nuevoUsuario = txtUsuario.getText().trim();
        String nuevaContraseña = txtContraseña.getText().trim();
        String nuevoTipo = cmbTipo.getSelectedItem().toString();
        
        try {
            boolean resultado = stub.modificarUsuario(
                usuarioActual, 
                nuevoUsuario.isEmpty() ? usuarioActual : nuevoUsuario,
                nuevaContraseña.isEmpty() ? null : nuevaContraseña,
                nuevoTipo.isEmpty() ? null : nuevoTipo
            );
            
            if (resultado) {
                JOptionPane.showMessageDialog(this, "Usuario modificado con éxito");
                actualizarListaUsuarios();
                limpiarCampos();
            } else {
                JOptionPane.showMessageDialog(this, "Error al modificar usuario. ¿El nuevo nombre ya existe?");
            }
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
    
    private void eliminarUsuario() {
        String usuario = listaUsuarios.getSelectedValue();
        if (usuario == null || usuario.startsWith("No hay")) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario para eliminar");
            return;
        }
        
        int confirmacion = JOptionPane.showConfirmDialog(
            this, 
            "¿Está seguro de eliminar al usuario " + usuario + "?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                boolean resultado = stub.eliminarUsuario(usuario);
                if (resultado) {
                    JOptionPane.showMessageDialog(this, "Usuario eliminado con éxito");
                    actualizarListaUsuarios();
                    limpiarCampos();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No se puede eliminar el usuario. Tiene citas pendientes o no existe.");
                }
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
    
    private void limpiarCampos() {
        txtUsuario.setText("");
        txtContraseña.setText("");
        cmbTipo.setSelectedIndex(0);
    }
}

class PanelAdministracion extends JPanel {
    private ICitas stub;
    private JTabbedPane pestañasUsuarios;
    
    public PanelAdministracion(ICitas stub) {
        this.stub = stub;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        pestañasUsuarios = new JTabbedPane();
        
        pestañasUsuarios.addTab("Registrar Usuario", crearPanelRegistro());
        pestañasUsuarios.addTab("Ver Doctores", crearPanelUsuarios("Doctor"));
        pestañasUsuarios.addTab("Ver Pacientes", crearPanelUsuarios("Paciente"));
        pestañasUsuarios.addTab("Ver Administradores", crearPanelUsuarios("Administrador"));
        
        add(pestañasUsuarios, BorderLayout.CENTER);
    }
    
    private JPanel crearPanelRegistro() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        JPanel panelFormulario = new JPanel(new GridLayout(4, 2, 10, 10));
        
        JTextField txtUsuario = new JTextField();
        JPasswordField txtContraseña = new JPasswordField();
        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"Doctor", "Paciente", "Administrador"});
        JButton btnRegistrar = new JButton("Registrar Usuario");
        
        panelFormulario.add(new JLabel("Usuario:"));
        panelFormulario.add(txtUsuario);
        panelFormulario.add(new JLabel("Contraseña:"));
        panelFormulario.add(txtContraseña);
        panelFormulario.add(new JLabel("Tipo:"));
        panelFormulario.add(cmbTipo);
        panelFormulario.add(new JLabel());
        panelFormulario.add(btnRegistrar);
        
        JTextArea txtResultado = new JTextArea(5, 30);
        txtResultado.setEditable(false);
        txtResultado.setLineWrap(true);
        txtResultado.setWrapStyleWord(true);
        
        btnRegistrar.addActionListener(e -> {
            try {
                String usuario = txtUsuario.getText().trim();
                String contraseña = new String(txtContraseña.getPassword()).trim();
                String tipo = (String) cmbTipo.getSelectedItem();
                
                if (usuario.isEmpty() || contraseña.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Usuario y contraseña son obligatorios");
                    return;
                }
                
                boolean resultado = stub.registrarUsuario(usuario, contraseña, tipo);
                
                if (resultado) {
                    txtResultado.setText("✅ Usuario registrado con éxito\n\n" +
                                      "Usuario: " + usuario + "\n" +
                                      "Tipo: " + tipo);
                    txtUsuario.setText("");
                    txtContraseña.setText("");
                    actualizarListasUsuarios();
                } else {
                    txtResultado.setText("❌ Error al registrar usuario\n\n" +
                                      "El nombre de usuario ya existe");
                }
            } catch (Exception ex) {
                txtResultado.setText("❌ Error: " + ex.getMessage());
            }
        });
        
        panel.add(panelFormulario, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtResultado), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel crearPanelUsuarios(String tipoUsuario) {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultListModel<String> modeloLista = new DefaultListModel<>();
        JList<String> listaUsuarios = new JList<>(modeloLista);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JButton btnActualizar = new JButton("Actualizar Lista");
        btnActualizar.addActionListener(e -> {
            try {
                modeloLista.clear();
                List<String> usuarios = stub.obtenerUsuariosPorTipo(tipoUsuario);
                if (usuarios.isEmpty()) {
                    modeloLista.addElement("No hay " + tipoUsuario.toLowerCase() + "s registrados");
                } else {
                    for (String usuario : usuarios) {
                        modeloLista.addElement(usuario);
                    }
                }
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + ex.getMessage());
            }
        });
        
        JPanel panelBoton = new JPanel();
        panelBoton.add(btnActualizar);
        
        panel.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        panel.add(panelBoton, BorderLayout.SOUTH);
        
        btnActualizar.doClick();
        
        return panel;
    }
    
    private void actualizarListasUsuarios() {
        for (int i = 1; i < pestañasUsuarios.getTabCount(); i++) {
            Component panel = pestañasUsuarios.getComponentAt(i);
            if (panel instanceof JPanel) {
                for (Component comp : ((JPanel)panel).getComponents()) {
                    if (comp instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) comp;
                        Component view = scrollPane.getViewport().getView();
                        if (view instanceof JList) {
                            JList<?> lista = (JList<?>) view;
                            for (Component buttonComp : ((JPanel)panel).getComponents()) {
                                if (buttonComp instanceof JButton) {
                                    ((JButton)buttonComp).doClick();
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}

class PanelDoctorCompleto extends JPanel {
    private ICitas stub;
    private String doctor;
    private JTable tablaCitas;
    private DefaultTableModel modeloTabla;
    private JTextField txtPaciente, txtFecha, txtHora, txtIdCita, txtAsunto;
    
    public PanelDoctorCompleto(ICitas stub, String doctor) {
        this.stub = stub;
        this.doctor = doctor;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnas = {"ID", "Paciente", "Fecha", "Hora", "Asunto", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaCitas = new JTable(modeloTabla);
        tablaCitas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCitas.getTableHeader().setReorderingAllowed(false);
        
        JPanel panelGestion = crearPanelGestion();
        
        JButton btnActualizar = new JButton("Actualizar Citas");
        btnActualizar.addActionListener(e -> actualizarTablaCitas());
        btnActualizar.setBackground(new Color(70, 130, 180));
        btnActualizar.setForeground(Color.WHITE);
        
        add(panelGestion, BorderLayout.NORTH);
        add(new JScrollPane(tablaCitas), BorderLayout.CENTER);
        add(btnActualizar, BorderLayout.SOUTH);
        
        actualizarTablaCitas();
    }
    
    private JPanel crearPanelGestion() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Gestión de Citas"));
        
        txtPaciente = new JTextField();
        txtFecha = new JTextField();
        txtHora = new JTextField();
        txtIdCita = new JTextField();
        txtAsunto = new JTextField();
        
        JButton btnCargar = new JButton("Cargar Cita");
        btnCargar.addActionListener(e -> {
            int filaSeleccionada = tablaCitas.getSelectedRow();
            if (filaSeleccionada >= 0) {
                txtIdCita.setText(tablaCitas.getValueAt(filaSeleccionada, 0).toString());
                txtPaciente.setText(tablaCitas.getValueAt(filaSeleccionada, 1).toString());
                txtFecha.setText(tablaCitas.getValueAt(filaSeleccionada, 2).toString());
                txtHora.setText(tablaCitas.getValueAt(filaSeleccionada, 3).toString());
                txtAsunto.setText(tablaCitas.getValueAt(filaSeleccionada, 4).toString());
            }
        });
        
        panel.add(new JLabel("ID Cita:"));
        panel.add(txtIdCita);
        panel.add(btnCargar);
        panel.add(new JLabel());
        panel.add(new JLabel("Paciente:"));
        panel.add(txtPaciente);
        panel.add(new JLabel("Fecha (YYYY-MM-DD):"));
        panel.add(txtFecha);
        panel.add(new JLabel("Hora (HH:MM):"));
        panel.add(txtHora);
        panel.add(new JLabel("Asunto:"));
        panel.add(txtAsunto);
        
        JButton btnAgendar = crearBotonAccion("Agendar Cita", Color.GREEN, e -> {
            if (validarCampos(txtPaciente, txtFecha, txtHora)) {
                try {
                    boolean resultado = stub.agendarCita(doctor, txtPaciente.getText(), 
                                                       txtFecha.getText(), txtHora.getText(),
                                                       txtAsunto.getText());
                    if (resultado) {
                        actualizarTablaCitas();
                        limpiarCampos();
                        JOptionPane.showMessageDialog(this, "Cita agendada con éxito");
                    } else {
                        JOptionPane.showMessageDialog(this, "Error al agendar cita");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
        
        JButton btnModificar = crearBotonAccion("Modificar Cita", Color.BLUE, e -> {
            if (validarCampos(txtIdCita, txtPaciente, txtFecha, txtHora)) {
                try {
                    int id = Integer.parseInt(txtIdCita.getText());
                    boolean resultado = stub.modificarCita(id, txtPaciente.getText(), 
                                                         txtFecha.getText(), txtHora.getText(),
                                                         txtAsunto.getText());
                    if (resultado) {
                        actualizarTablaCitas();
                        limpiarCampos();
                        JOptionPane.showMessageDialog(this, "Cita modificada con éxito");
                    } else {
                        JOptionPane.showMessageDialog(this, "Error al modificar cita");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
        
        JButton btnCancelar = crearBotonAccion("Cancelar Cita", Color.ORANGE, e -> {
            if (validarCampos(txtIdCita)) {
                try {
                    int id = Integer.parseInt(txtIdCita.getText());
                    boolean resultado = stub.cancelarCita(id);
                    if (resultado) {
                        actualizarTablaCitas();
                        limpiarCampos();
                        JOptionPane.showMessageDialog(this, "Cita cancelada con éxito");
                    } else {
                        JOptionPane.showMessageDialog(this, "Error al cancelar cita");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
        
        JButton btnEliminar = crearBotonAccion("Eliminar Cita", Color.RED, e -> {
            if (validarCampos(txtIdCita)) {
                try {
                    int id = Integer.parseInt(txtIdCita.getText());
                    boolean eliminada = stub.eliminarCita(id);
                    if (eliminada) {
                        actualizarTablaCitas();
                        limpiarCampos();
                        JOptionPane.showMessageDialog(this, "Cita eliminada permanentemente");
                    } else {
                        JOptionPane.showMessageDialog(this, "Error: Solo se pueden eliminar citas canceladas");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
        
        JButton btnReactivar = crearBotonAccion("Reactivar Cita", new Color(0, 128, 128), e -> {
            if (validarCampos(txtIdCita)) {
                try {
                    int id = Integer.parseInt(txtIdCita.getText());
                    boolean resultado = stub.reactivarCita(id);
                    if (resultado) {
                        actualizarTablaCitas();
                        JOptionPane.showMessageDialog(this, "Cita reactivada con éxito");
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Error: No se pudo reactivar. Verifique:\n" +
                            "1. Que la cita esté cancelada\n" +
                            "2. Que el horario siga disponible");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
        
        panel.add(btnAgendar);
        panel.add(btnModificar);
        panel.add(btnCancelar);
        panel.add(btnEliminar);
        panel.add(btnReactivar);
        
        return panel;
    }
    
    private JButton crearBotonAccion(String texto, Color color, ActionListener accion) {
        JButton boton = new JButton(texto);
        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.addActionListener(accion);
        return boton;
    }
    
    private boolean validarCampos(JTextField... campos) {
        for (JTextField campo : campos) {
            if (campo.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor complete todos los campos requeridos");
                campo.requestFocus();
                return false;
            }
        }
        return true;
    }
    
    private void limpiarCampos() {
        txtIdCita.setText("");
        txtPaciente.setText("");
        txtFecha.setText("");
        txtHora.setText("");
        txtAsunto.setText("");
    }
    
    private void actualizarTablaCitas() {
        try {
            modeloTabla.setRowCount(0);
            List<String> citas = stub.verCitasDoctor(doctor);
            
            for (String citaStr : citas) {
                String[] partes = citaStr.split("\\|");
                String idParte = partes[0].trim();
                String fechaHoraParte = partes[1].trim();
                String asuntoParte = partes.length > 2 ? partes[2].trim() : "Asunto: ";
                String estadoParte = partes.length > 3 ? partes[3].trim() : "Estado: pendiente";
                
                String id = idParte.substring(idParte.indexOf('[') + 1, idParte.indexOf(']'));
                String paciente = idParte.split("-")[0].replaceFirst("\\[\\d+\\]\\s*", "").trim();
                
                String[] fechaHora = fechaHoraParte.split("\\s+");
                String fecha = fechaHora[0];
                String hora = fechaHora.length > 1 ? fechaHora[1] : "";
                
                String asunto = asuntoParte.replace("Asunto:", "").trim();
                String estado = estadoParte.replace("Estado:", "").trim();
                
                modeloTabla.addRow(new Object[]{id, paciente, fecha, hora, asunto, estado});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + ex.getMessage());
        }
    }
}

class PanelSolicitarCita extends JPanel {
    private ICitas stub;
    private String paciente;
    private JTextField txtDoctor, txtFecha, txtHora, txtAsunto;
    
    public PanelSolicitarCita(ICitas stub, String paciente) {
        this.stub = stub;
        this.paciente = paciente;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel panelFormulario = new JPanel(new GridLayout(4, 2, 10, 10));
        
        txtDoctor = new JTextField();
        txtFecha = new JTextField();
        txtHora = new JTextField();
        txtAsunto = new JTextField();
        
        panelFormulario.add(new JLabel("Doctor:"));
        panelFormulario.add(txtDoctor);
        panelFormulario.add(new JLabel("Fecha (YYYY-MM-DD):"));
        panelFormulario.add(txtFecha);
        panelFormulario.add(new JLabel("Hora (HH:MM):"));
        panelFormulario.add(txtHora);
        panelFormulario.add(new JLabel("Asunto:"));
        panelFormulario.add(txtAsunto);
        
        JPanel panelBoton = new JPanel();
        JButton btnSolicitar = new JButton("Solicitar Cita");
        btnSolicitar.setBackground(new Color(34, 139, 34));
        btnSolicitar.setForeground(Color.WHITE);
        btnSolicitar.setPreferredSize(new Dimension(150, 30));
        
        JPanel panelResultado = new JPanel(new BorderLayout());
        JTextArea txtResultado = new JTextArea(3, 30);
        txtResultado.setEditable(false);
        txtResultado.setLineWrap(true);
        txtResultado.setWrapStyleWord(true);
        txtResultado.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelResultado.add(new JLabel("Resultado:"), BorderLayout.NORTH);
        panelResultado.add(new JScrollPane(txtResultado), BorderLayout.CENTER);
        
        btnSolicitar.addActionListener(e -> {
            if (txtDoctor.getText().trim().isEmpty() || 
                txtFecha.getText().trim().isEmpty() || 
                txtHora.getText().trim().isEmpty()) {
                
                JOptionPane.showMessageDialog(this, 
                    "Por favor complete todos los campos obligatorios", 
                    "Campos incompletos", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                if (!stub.existeDoctor(txtDoctor.getText().trim())) {
                    txtResultado.setText("❌ El doctor no existe\n\n" +
                                    "Por favor verifique el nombre del doctor\n" +
                                    "y vuelva a intentar");
                    return;
                }
                
                boolean res = stub.solicitarCita(paciente, 
                    txtDoctor.getText().trim(), 
                    txtFecha.getText().trim(), 
                    txtHora.getText().trim(),
                    txtAsunto.getText().trim());
                
                if (res) {
                    txtResultado.setText("✅ Cita solicitada con éxito\n\n" +
                                       "Doctor: " + txtDoctor.getText() + "\n" +
                                       "Fecha: " + txtFecha.getText() + "\n" +
                                       "Hora: " + txtHora.getText() + "\n" +
                                       "Asunto: " + txtAsunto.getText());
                    
                    txtDoctor.setText("");
                    txtFecha.setText("");
                    txtHora.setText("");
                    txtAsunto.setText("");
                } else {
                    txtResultado.setText("⚠️ No disponible en ese horario\n\n" +
                                       "El doctor no tiene disponibilidad\n" +
                                       "en la fecha y hora especificadas");
                }
            } catch (Exception ex) {
                txtResultado.setText("❌ Error al solicitar cita\n\n" + ex.getMessage());
            }
        });
        
        panelBoton.add(btnSolicitar);
        
        JPanel panelCentral = new JPanel(new BorderLayout(10, 20));
        panelCentral.add(panelFormulario, BorderLayout.NORTH);
        panelCentral.add(panelBoton, BorderLayout.CENTER);
        
        add(panelCentral, BorderLayout.NORTH);
        add(panelResultado, BorderLayout.CENTER);
        
        txtHora.addActionListener(btnSolicitar.getActionListeners()[0]);
    }
}

class PanelCitasPaciente extends JPanel {
    private ICitas stub;
    private String paciente;
    
    public PanelCitasPaciente(ICitas stub, String paciente) {
        this.stub = stub;
        this.paciente = paciente;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnas = {"ID", "Doctor", "Fecha", "Hora", "Asunto", "Estado"};
        DefaultTableModel modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable tablaCitas = new JTable(modeloTabla);
        tablaCitas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JButton btnActualizar = new JButton("Actualizar Mis Citas");
        btnActualizar.setBackground(new Color(70, 130, 180));
        btnActualizar.setForeground(Color.WHITE);
        btnActualizar.addActionListener(e -> {
            try {
                modeloTabla.setRowCount(0);
                List<String> citas = stub.verCitasPaciente(paciente);
                
                if (citas.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No tienes citas agendadas.");
                } else {
                    for (String citaStr : citas) {
                        String[] partes = citaStr.split("\\|");
                        String idParte = partes[0].trim();
                        String fechaHoraParte = partes[1].trim();
                        String asuntoParte = partes.length > 2 ? partes[2].trim() : "Asunto: ";
                        String estadoParte = partes.length > 3 ? partes[3].trim() : "Estado: pendiente";
                        
                        String id = idParte.substring(idParte.indexOf('[') + 1, idParte.indexOf(']'));
                        String doctor = idParte.split("-")[1].replace("Dr.", "").trim();
                        
                        String[] fechaHora = fechaHoraParte.split("\\s+");
                        String fecha = fechaHora[0];
                        String hora = fechaHora.length > 1 ? fechaHora[1] : "";
                        
                        String asunto = asuntoParte.replace("Asunto:", "").trim();
                        String estado = estadoParte.replace("Estado:", "").trim();
                        
                        modeloTabla.addRow(new Object[]{id, doctor, fecha, hora, asunto, estado});
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar citas: " + ex.getMessage());
            }
        });
        
        add(new JScrollPane(tablaCitas), BorderLayout.CENTER);
        
        JPanel panelBoton = new JPanel();
        panelBoton.add(btnActualizar);
        add(panelBoton, BorderLayout.SOUTH);
        
        btnActualizar.doClick();
    }
}

class PanelBuscarCitas extends JPanel {
    private ICitas stub;
    
    public PanelBuscarCitas(ICitas stub) {
        this.stub = stub;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columnas = {"ID", "Paciente", "Doctor", "Fecha", "Hora", "Asunto", "Estado"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable tablaResultados = new JTable(modelo);
        
        JTextField txtBusqueda = new JTextField();
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setBackground(new Color(70, 130, 180));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.addActionListener(e -> {
            try {
                modelo.setRowCount(0);
                List<String> resultados = stub.buscarCitas(txtBusqueda.getText());
                
                if (resultados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron citas que coincidan con la búsqueda.");
                } else {
                    for (String citaStr : resultados) {
                        String[] partes = citaStr.split("\\|");
                        String idParte = partes[0].trim();
                        String fechaHoraParte = partes[1].trim();
                        String asuntoParte = partes.length > 2 ? partes[2].trim() : "Asunto: ";
                        String estadoParte = partes.length > 3 ? partes[3].trim() : "Estado: pendiente";
                        
                        String id = idParte.substring(idParte.indexOf('[') + 1, idParte.indexOf(']'));
                        String paciente = idParte.split("-")[0].replaceFirst("\\[\\d+\\]\\s*", "").trim();
                        String doctor = idParte.split("-")[1].replace("Dr.", "").trim();
                        
                        String[] fechaHora = fechaHoraParte.split("\\s+");
                        String fecha = fechaHora[0];
                        String hora = fechaHora.length > 1 ? fechaHora[1] : "";
                        
                        String asunto = asuntoParte.replace("Asunto:", "").trim();
                        String estado = estadoParte.replace("Estado:", "").trim();
                        
                        modelo.addRow(new Object[]{id, paciente, doctor, fecha, hora, asunto, estado});
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        
        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 5));
        panelBusqueda.add(new JLabel("Buscar (paciente, doctor o fecha):"), BorderLayout.WEST);
        panelBusqueda.add(txtBusqueda, BorderLayout.CENTER);
        panelBusqueda.add(btnBuscar, BorderLayout.EAST);
        
        txtBusqueda.addActionListener(btnBuscar.getActionListeners()[0]);
        
        add(panelBusqueda, BorderLayout.NORTH);
        add(new JScrollPane(tablaResultados), BorderLayout.CENTER);
    }
}
