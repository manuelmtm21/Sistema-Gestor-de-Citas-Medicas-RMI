Sistema Gestor de Citas Médicas Distribuido (Java RMI)

¿Qué es y cuál es su propósito?
Este proyecto es una aplicación distribuida desarrollada en Java que simula un entorno hospitalario realista para la gestión de citas médicas.
El sistema permite a administradores, doctores y pacientes interactuar en tiempo real sobre una misma base de datos.

El núcleo tecnológico de esta aplicación es Java RMI (Remote Method Invocation).
Esta tecnología es la que permite que el programa Cliente (la interfaz gráfica) invoque los métodos del Servidor remoto a través de la red de área local, logrando que interactúen como si estuvieran ejecutándose en la misma computadora.

¿Cómo probar el proyecto? (Guía Rápida)
La arquitectura cliente-servidor requiere inicializar primero el servidor de manera obligatoria para que el cliente tenga a dónde conectarse.
1. Abre una terminal y arranca el registro de RMI con el comando start rmiregistry.
2. En la terminal, compila e inicia el servidor ejecutando java Servidor.Servidor. El sistema te mostrará automáticamente la IP local de tu máquina.
3. Ejecuta la interfaz gráfica del cliente con java Cliente.Cliente. Al iniciar, la aplicación te pedirá ingresar la IP del servidor que obtuviste en el paso anterior.
4. Inicia sesión. Si el sistema detecta que no hay datos guardados, generará automáticamente usuarios por defecto para facilitar las pruebas.
Puedes usar estas credenciales:
• Administrador: Usuario: admin | Contraseña: admin123
• Doctor: Usuario: Dr. Omar | Contraseña: doc123
• Paciente: Usuario: Manuel | Contraseña: 123

¿Cómo funciona?
(Funciones Principales)
El sistema cuenta con un control de acceso basado en roles.
Dependiendo del tipo de usuario que inicie sesión, la interfaz gráfica de Java Swing se adaptará para mostrar distintas herramientas:

1. Panel de Administrador (Gestión Total)
• Tiene el control absoluto de la base de datos para registrar, modificar o eliminar Doctores, Pacientes u otros Administradores del sistema.
• Cuenta con un sistema de pestañas para visualizar listas filtradas por tipo de usuario.
• Tiene acceso a una herramienta de búsqueda global para auditar citas médicas registradas en la clínica filtrando por paciente, doctor o fecha.

2. Panel de Doctor (Control de Agenda)
• Cada médico tiene un panel personalizado donde visualiza en una tabla dinámica únicamente a los pacientes que le han solicitado consulta.
• Los doctores tienen los permisos necesarios para agendar nuevas citas, modificar horarios de consultas existentes, o cancelarlas.
• El sistema previene el borrado accidental: las citas solo se pueden eliminar permanentemente de la base de datos si previamente fueron marcadas como "canceladas".

3. Panel de Paciente (Autoservicio)
• Los pacientes pueden solicitar una cita ingresando el nombre exacto de su doctor, la fecha, la hora y el motivo de la consulta.
• El sistema cuenta con validación de disponibilidad en tiempo real: antes de guardar el registro, el servidor comprueba que el doctor elegido no tenga ya una consulta en esa misma fecha y hora.
• Poseen una pestaña exclusiva de "Mis Citas" para monitorear el historial y el estado (pendiente o cancelada) de sus consultas

4. Arquitectura y Persistencia de Datos
• Persistencia Segura: El servidor serializa los objetos (las listas de Usuarios y Citas) y los guarda de forma local en un único archivo llamado citas.dat.
• Transacciones Atómicas: Para evitar que la base de datos se corrompa si ocurre un error inesperado, el sistema guarda la información primero en un archivo temporal (.tmp) y posteriormente reemplaza el archivo original de forma segura.
• Concurrencia: Los métodos de guardado y carga en el servidor utilizan la palabra reservada synchronized. Esto permite que múltiples clientes se conecten y guarden datos al mismo tiempo sin crear conflictos.
