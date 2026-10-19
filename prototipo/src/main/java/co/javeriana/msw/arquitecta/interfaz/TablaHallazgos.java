package co.javeriana.msw.arquitecta.interfaz;

import co.javeriana.msw.arquitecta.reglas.Violacion;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Tabla de hallazgos: severidad, regla, descripción y paquetes implicados. */
final class TablaHallazgos extends JTable {

    private static final long serialVersionUID = 1L;

    private final Modelo modelo;

    TablaHallazgos() {
        this(new Modelo());
    }

    private TablaHallazgos(Modelo modelo) {
        super(modelo);
        this.modelo = modelo;
        setRowHeight(32);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFont(Tema.texto(13f));
        setForeground(Tema.TEXTO);
        setBackground(Tema.SUPERFICIE);
        setSelectionBackground(Tema.PRIMARIO_SUAVE);
        setSelectionForeground(Tema.TEXTO);
        JTableHeader cabecera = getTableHeader();
        cabecera.setFont(Tema.negrita(12f));
        cabecera.setReorderingAllowed(false);
        cabecera.setPreferredSize(new Dimension(0, 32));
        // El encabezado del aspecto del sistema no siempre respeta los colores: se dibuja uno propio.
        cabecera.setDefaultRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco,
                    int fila, int columna) {
                super.getTableCellRendererComponent(t, valor, false, false, fila, columna);
                setFont(Tema.negrita(12f));
                setForeground(Tema.TEXTO_SUAVE);
                setBackground(Tema.LATERAL);
                setOpaque(true);
                setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                        javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10)));
                return this;
            }
        });

        getColumnModel().getColumn(0).setPreferredWidth(110);
        getColumnModel().getColumn(0).setMaxWidth(130);
        getColumnModel().getColumn(1).setPreferredWidth(230);
        getColumnModel().getColumn(2).setPreferredWidth(420);
        getColumnModel().getColumn(3).setPreferredWidth(320);
        getColumnModel().getColumn(0).setCellRenderer(new RendererSeveridad());
        DefaultTableCellRenderer texto = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco,
                    int fila, int columna) {
                super.getTableCellRendererComponent(t, valor, sel, false, fila, columna);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setBackground(sel ? Tema.PRIMARIO_SUAVE : fila % 2 == 0 ? Tema.SUPERFICIE : new Color(0xF8FAFC));
                setForeground(columna == 3 ? Tema.TEXTO_SUAVE : Tema.TEXTO);
                setToolTipText(valor == null ? null : valor.toString());
                return this;
            }
        };
        for (int c = 1; c < 4; c++) {
            getColumnModel().getColumn(c).setCellRenderer(texto);
        }
    }

    void mostrar(List<Violacion> violaciones) {
        modelo.filas = new ArrayList<>(violaciones);
        modelo.fireTableDataChanged();
    }

    Violacion seleccionada() {
        int fila = getSelectedRow();
        return fila < 0 ? null : modelo.filas.get(convertRowIndexToModel(fila));
    }

    private static final class Modelo extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"Severidad", "Regla", "Descripción", "Paquetes implicados"};
        private List<Violacion> filas = new ArrayList<>();

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Violacion v = filas.get(fila);
            return switch (columna) {
                case 0 -> v.severidad();
                case 1 -> v.regla();
                case 2 -> v.descripcion();
                default -> String.join(", ", v.ubicacion());
            };
        }
    }

    /** Dibuja la severidad como una etiqueta de color. */
    private static final class RendererSeveridad implements javax.swing.table.TableCellRenderer {

        private final Chip chip = new Chip(11.5f);
        private final javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 4));

        RendererSeveridad() {
            panel.add(chip);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco,
                int fila, int columna) {
            Violacion.Severidad s = (Violacion.Severidad) valor;
            chip.mostrar(s.etiqueta(), Tema.colorSeveridad(s), Tema.fondoSeveridad(s));
            panel.setBackground(sel ? Tema.PRIMARIO_SUAVE : fila % 2 == 0 ? Tema.SUPERFICIE : new Color(0xF8FAFC));
            return panel;
        }
    }
}
