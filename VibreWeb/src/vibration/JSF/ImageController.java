package vibration.JSF;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.imageio.ImageIO;
import javax.inject.Named;
import javax.servlet.http.Part;

import com.sun.media.jfxmedia.logging.Logger;

import vibration.EJB.MetingEJBLocal;
import vibration.EJB.UserManagementEJBLocal;
import vibration.JPA.Experimenten;
import vibration.JPA.Foto;
import vibration.JPA.Project;

@Named("imageController")
@ViewScoped
public class ImageController implements Serializable {

	private static final long serialVersionUID = -3180675991359372164L;

	@EJB
	private UserManagementEJBLocal userEJB;

	@EJB
	private MetingEJBLocal metingEJB;

	private String imageString = "Test";
	private List<Foto> fotoData;
	private Experimenten experiment = new Experimenten();
	private Project project = new Project();
	private Part file;
	private String fileContent;
	private String naam;
	private String foutMessage;

	private List<Experimenten> experiments = new ArrayList<>();

	// verwijderen 1 tot meerdere experimenten.
	public void verwijderExperiment() {
		experiment = userEJB.findExperiment(experiment.getId());
		int projectId = experiment.getProject().getId();
		metingEJB.verwijderExperiment(experiment.getId());
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("/VibreWeb/users/project.xhtml?project=" + projectId);
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());

		}
	}

	public void verwijderSelectedExperiments() {
		for (Experimenten exp : experiments) {
			metingEJB.verwijderExperiment(exp.getId());
		}
	}

	// ophalen foto data voor tabellen
	public void getProjData() {
		if (project.getId() > 0) {
			userEJB.setExperiment(false);
			userEJB.setProject(true);
			setFotoData(userEJB.fotoOphalen(project.getId()));
		}
	}

	public void getExpData() {
		if (experiment.getId() > 0) {
			userEJB.setProject(false);
			userEJB.setExperiment(true);
			setFotoData(userEJB.fotoOphalen(experiment.getId()));
		}
	}

	// navigatie naar paginas met projecten
	public void naarProjectPagina() {
		experiment = userEJB.findExperiment(experiment.getId());
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("publicProject.xhtml?project=" + experiment.getProject().getId());
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());

		}
	}

	public void naarEigenProjectPagina() {
		experiment = userEJB.findExperiment(experiment.getId());
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("/VibreWeb/users/project.xhtml?project=" + experiment.getProject().getId());
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());

		}
	}

	public void naarProfielPagina() {
		project = userEJB.findProject(project.getId());
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("profielpaginaBezoekers.xhtml?gebruiker=" + project.getPersonen().getIdpersonen());
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());

		}
	}

	public void naarEigenProfielPagina() {
		project = userEJB.findProject(project.getId());
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("/VibreWeb/spotter/profielpagina.xhtml");
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());

		}
	}

	// uploaden fotos + navigatie naar uploadpagina
	public void uploadPaginaProj() {
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("/VibreWeb/users/ImageUpload.xhtml?project=" + project.getId());
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());
		}

	}

	public void uploadPaginaExp() {
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("/VibreWeb/users/ImageUpload.xhtml?experiment=" + experiment.getId());
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());
		}
	}

	
	public byte[] scale(byte[] fileData, int width, int height) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(fileData);
        try {
            BufferedImage img = ImageIO.read(in);
            if(height == 0) {
                height = (width * img.getHeight())/ img.getWidth(); 
            }
            if(width == 0) {
                width = (height * img.getWidth())/ img.getHeight();
            }
            Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage imageBuff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            imageBuff.getGraphics().drawImage(scaledImage, 0, 0, new Color(0,0,0), null);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            ImageIO.write(imageBuff, "jpg", buffer);

            return buffer.toByteArray();
        } catch (IOException e) {
			throw new IOException("Afbeelding scalen mislukt");
        }
    }
	
	public void upload() throws IOException {
		InputStream inputStream = null;
		try {
			if (file == null) {
				foutMessage = "Gelieve een afbeelding te selecteren.";
				return;
			} else {
				inputStream = file.getInputStream();
			}
		} catch (IOException e) {
			throw new IOException("Afbeelding selecteren mislukt");
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int read = 0;
		final byte[] bytes = new byte[16177215];

		try {
			while ((read = inputStream.read(bytes)) != -1) {
				buffer.write(bytes, 0, read);
			}
		} catch (IOException e) {
			Logger.logMsg(1, e.toString());
		}

		byte[] image = buffer.toByteArray();

		byte[] rescaledImage = scale(image,200,200);
 
		if (rescaledImage.length > 2097000) {
			foutMessage = "Afbeelding te groot, gelieve een kleinere te selecteren.";
			return;
		} else {
			foutMessage = null;
			if (project.getId() != 0 && experiment.getId() == 0) {
				userEJB.uploadenFoto(rescaledImage, naam, project.getId(), 0);
				try {
					FacesContext.getCurrentInstance().getExternalContext()
							.redirect("/VibreWeb//users/project.xhtml?project=" + project.getId());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Logger.logMsg(1, e.toString());
				}
			} else if (project.getId() == 0 && experiment.getId() != 0) {
				userEJB.uploadenFoto(rescaledImage, naam, 0, experiment.getId());
				try {

					FacesContext.getCurrentInstance().getExternalContext()
							.redirect("/VibreWeb//users/experiment.xhtml?experiment=" + experiment.getId());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Logger.logMsg(1, e.toString());
				}
			}
		}
	}

	// getters en setters
	public Part getFile() {
		return file;
	}

	public void setFile(Part p) {
		file = p;
	}

	public String getFileContent() {
		return fileContent;
	}

	public void setFileContent(String f) {
		fileContent = f;
	}

	public String getImage() {
		return imageString;
	}

	public void setImage(String image) {
		this.imageString = image;
	}

	public List<Foto> getFotoData() {
		return fotoData;
	}

	public void setFotoData(List<Foto> fotoData) {
		this.fotoData = fotoData;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Experimenten getExperiment() {
		return experiment;
	}

	public void setExperiment(Experimenten experiment) {
		this.experiment = experiment;
	}

	public String getFoutMessage() {
		return foutMessage;
	}

	public void setFoutMessage(String s) {
		foutMessage = s;
	}

	public String getNaam() {
		return naam;
	}

	public void setNaam(String s) {
		naam = s;
	}

	public List<Experimenten> getExperiments() {
		return experiments;
	}

	public void setExperiments(List<Experimenten> experiments) {
		this.experiments = experiments;
	}

}
