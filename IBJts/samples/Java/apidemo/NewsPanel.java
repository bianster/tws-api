/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.ib.client.Contract;
import com.ib.client.MarketDataType;
import com.ib.client.Util;
import com.ib.client.Types.SecType;
import com.ib.controller.ApiController.ITickNewsHandler;

import apidemo.util.HtmlButton;
import apidemo.util.NewTabbedPanel;
import apidemo.util.TCombo;
import apidemo.util.NewTabbedPanel.NewTabPanel;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;

public class NewsPanel extends JPanel {
    private final NewTabbedPanel m_requestPanels = new NewTabbedPanel();
    private final NewTabbedPanel m_resultsPanels = new NewTabbedPanel();

    NewsPanel() {
        m_requestPanels.addTab( "News Ticks", new NewsTicksRequestPanel() );

        setLayout( new BorderLayout() );
        add( m_requestPanels, BorderLayout.NORTH);
        add( m_resultsPanels);
    }

    class NewsTicksRequestPanel extends JPanel {
        private UpperField m_symbol = new UpperField();
        private TCombo<SecType> m_secType = new TCombo<SecType>( SecType.values() );
        private UpperField m_exchange = new UpperField();
        private UpperField m_primExchange = new UpperField();
        private UpperField m_currency = new UpperField();

        NewsTicksRequestPanel() {
            m_symbol.setText( "IBKR");
            m_secType.setSelectedItem( SecType.STK);
            m_exchange.setText( "SMART"); 
            m_primExchange.setText( "NYSE"); 
            m_currency.setText( "USD");

            HtmlButton but = new HtmlButton( "Request News Ticks") {
                @Override protected void actionPerformed() {
                    onRequestNewsTicks();
                }
            };

            VerticalPanel topPanel = new VerticalPanel();
            topPanel.add( "Symbol", m_symbol);
            topPanel.add( "Sec Type", m_secType);
            topPanel.add( "Exchange", m_exchange, Box.createHorizontalStrut(30), but);
            topPanel.add( "Prim Exch", m_primExchange);
            topPanel.add( "Currency", m_currency);
            setLayout( new BorderLayout() );
            add( topPanel, BorderLayout.NORTH);
        }

        protected void onRequestNewsTicks() {
            Contract contract = new Contract();
            contract.symbol( m_symbol.getText().toUpperCase() ); 
            contract.secType( m_secType.getSelectedItem() ); 
            contract.exchange( m_exchange.getText().toUpperCase() ); 
            contract.primaryExch( m_primExchange.getText().toUpperCase() ); 
            contract.currency( m_currency.getText().toUpperCase() ); 

            NewsTicksResultsPanel panel = new NewsTicksResultsPanel();
            m_resultsPanels.addTab( "News Ticks: " + contract.symbol(), panel, true, true);
            ApiDemo.INSTANCE.controller().reqNewsTicks(contract, panel);
        }
    }

    class NewsTicksResultsPanel extends NewTabPanel implements ITickNewsHandler {
        final NewsTicksModel m_model = new NewsTicksModel();
        final ArrayList<NewsTickRow> m_rows = new ArrayList<NewsTickRow>();

        NewsTicksResultsPanel() {
            JTable table = new JTable( m_model);
            table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
                public void valueChanged(ListSelectionEvent event) {
                    if (!event.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                        NewsTickRow newsTickRow = m_rows.get( table.getSelectedRow());
                    	if (newsTickRow.m_providerCode.length() > 0 && newsTickRow.m_articleId.length() > 0) {
                            m_requestPanels.select( "News Article");
                        }
                    }
                }
            });
            table.getColumnModel().getColumn(3).setMinWidth(550);
            JScrollPane scroll = new JScrollPane( table);
            setLayout( new BorderLayout() );
            add( scroll);
        };

        /** Called when the tab is first visited. */
        @Override public void activated() { /* noop */ }

        /** Called when the tab is closed by clicking the X. */
        @Override public void closed() { /* noop */ }

        @Override
        public void tickNews(long timeStamp, String providerCode, String articleId, String headline, String extraData) {
            NewsTickRow newsTickRow = new NewsTickRow(timeStamp, providerCode, articleId, headline, extraData);
            m_rows.add( newsTickRow);
            fire();
        }

        private void fire() {
            SwingUtilities.invokeLater( new Runnable() {
                @Override public void run() {
                    m_model.fireTableRowsInserted( m_rows.size() - 1, m_rows.size() - 1);
                    revalidate();
                    repaint();
                }
            });
        }

        class NewsTicksModel extends AbstractTableModel {
            @Override public int getRowCount() {
                return m_rows.size();
            }

            @Override public int getColumnCount() {
                return 5;
            }

            @Override public String getColumnName(int col) {
                switch( col) {
                    case 0: return "Time Stamp";
                    case 1: return "Provider Code";
                    case 2: return "Article Id";
                    case 3: return "Headline";
                    case 4: return "Extra Data";
                    default: return null;
                }
            }

            @Override public Object getValueAt(int rowIn, int col) {
                NewsTickRow newsTickRow = m_rows.get( rowIn);
                switch( col) {
                    case 0: return newsTickRow.m_timeStamp;
                    case 1: return newsTickRow.m_providerCode;
                    case 2: return newsTickRow.m_articleId;
                    case 3: return newsTickRow.m_headline;
                    case 4: return newsTickRow.m_extraData;
                    default: return null;
                }
            }
        }

        class NewsTickRow {
            String m_timeStamp;
            String m_providerCode;
            String m_articleId;
            String m_headline;
            String m_extraData;

            public NewsTickRow(long timeStamp, String providerCode, String articleId, String headline, String extraData) {
                update( timeStamp, providerCode, articleId, headline, extraData);
            }

            void update( long timeStamp, String providerCode, String articleId, String headline, String extraData) {
                m_timeStamp = Util.UnixMillisecondsToString(timeStamp, "yyyy-MM-dd HH:mm:ss zzz");
                m_providerCode = providerCode;
                m_articleId = articleId;
                m_headline = headline;
                m_extraData = extraData;
            }
        }
    }
}
