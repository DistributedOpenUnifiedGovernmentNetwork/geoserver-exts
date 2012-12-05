package org.geoserver.cluster.hazelcast.web;

import static org.geoserver.cluster.hazelcast.HazelcastUtil.localIPAsString;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.SimpleAjaxLink;

import com.hazelcast.core.HazelcastInstance;

public class NodeLinkPanel extends Panel {

    HazelcastInstance hz;
    GeoServerDialog dialog;

    @SuppressWarnings("unchecked")
    public NodeLinkPanel(String id, HazelcastInstance hz) {
        super(id);

        add(new SimpleAjaxLink("link", new Model(localIPAsString(hz))) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                //dialog.show(target);
                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                    
                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        return true;
                    }
                    
                    @Override
                    protected Component getContents(String id) {
                        return new NodeInfoDialog(id);
                    }
                });
            }
        });

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(255);
        dialog.setInitialWidth(300);
    }

}
