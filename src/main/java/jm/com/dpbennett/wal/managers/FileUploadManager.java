/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.com.dpbennett.wal.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import jm.com.dpbennett.wal.utils.PrimeFacesUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Desmond Bennett <info@dpbennett.com.jm at http//dpbennett.com.jm>
 */
public class FileUploadManager {

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void upload() {
        if (file != null) {
            FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " was uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        FacesMessage msg = new FacesMessage("Succesful",
                event.getFile().getFileName() + " was uploaded.");

        try {
            System.out.println("Saving " + event.getFile().getFileName() + " ...");
            OutputStream outputStream;

            File fileToSave = new File("/home/desbenn/Projects/purchase-management/doc/" + event.getFile().getFileName());
            outputStream = new FileOutputStream(fileToSave);

            outputStream.write(event.getFile().getContents());

            outputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(FileUploadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void okAttachment() {
        //tk
        // The return of this dialog should be handled by "dialogReturn" event?
        // for now just close the dialog.
        System.out.println("ok attachment...");
        PrimeFacesUtils.closeDialog(null);

    }

    public void closeDialog() {
        PrimeFacesUtils.closeDialog(null);
    }
}
