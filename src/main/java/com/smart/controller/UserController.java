package com.smart.controller;

import java.util.List;
import java.util.Optional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

import javax.servlet.http.HttpSession;

import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy.UpdateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.objenesis.instantiator.basic.NewInstanceInstantiator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contacts;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	//method for sending common data with every other handler
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		String userName = principal.getName();
		System.out.println("UserName: "+userName);
		
		User user=userRepository.getUserByUserName(userName);
		System.out.println("User:"+user);
		model.addAttribute("user",user);
		
	}
	
	//user dashboard controller
	@RequestMapping("/index")
	public String dashBoard(Model model,Principal principal) {
		model.addAttribute("title","Home");
		return "normal/user_dashboard";
	}
	
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contacts());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
					@ModelAttribute Contacts contacts,
					@RequestParam("profileImage") MultipartFile multipartFile,
					Principal principal,
					HttpSession session) {
		try {
			//get the user name
			String name=principal.getName();
			//get the user details using user name we got above
			User user=this.userRepository.getUserByUserName(name);
			//process the image file
			if(multipartFile.isEmpty()) {
				System.out.println("File is empty!");
				contacts.setContactImage("contact.png");
			}else {
				contacts.setContactImage(multipartFile.getOriginalFilename());
				File saveFile=new ClassPathResource("static/images").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+multipartFile.getOriginalFilename());
				Files.copy(multipartFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image is uploaded");
			}
			//add the contact to user's contact list
			user.getConctacts().add(contacts);
			contacts.setUser(user);
			//save the user with new contact details
			this.userRepository.save(user);
			
			System.out.println("Contact added to database.");
			session.setAttribute("message", new Message("Contact has been added successfully!", "success"));
			
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message", new Message("something went wrong ! Try again", "danger"));
		}
		return "normal/add_contact_form";
	}
	
	//show contacts handler
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model,Principal principal) {
		//send the title name 
		model.addAttribute("title","Show contacts ");
		//get the userName first to get the user Id latter for query
		String userNameString=principal.getName();
		//get the user details using his userName
		User user= this.userRepository.getUserByUserName(userNameString);
		//getting page and no. of pages
		Pageable pageable=PageRequest.of(page, 1);
		//now we will get the 5 contacts per page
		Page<Contacts> contacts=this.contactRepository.findContactsByUserId(user.getId(),pageable);
		//now set the contacts with model attribute
		model.addAttribute("contacts",contacts);
		//also send the page no. that is current page
		model.addAttribute("currentPage",page);
		//also total no. of pages that has been created
		model.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing particular contact details
	@RequestMapping("/contact/{cId}")
	public String showContactDetail(
			@PathVariable("cId") Integer cId,
			Model model,
			Principal principal
			) {
		
		Optional<Contacts> contactOptional = this.contactRepository.findById(cId);
		Contacts contacts= contactOptional.get();
		String userNameString=principal.getName();
		User user=this.userRepository.getUserByUserName(userNameString);
		if(user.getId()==contacts.getUser().getId()) {
			model.addAttribute("contacts",contacts);
			model.addAttribute("title",contacts.getFirstName());
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cId}")
	public String delete(
			@PathVariable("cId") Integer cId, 
			Model model,
			Principal principal,
			HttpSession session) {
		Contacts contacts=this.contactRepository.findById(cId).get();
		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getConctacts().remove(contacts);
		session.setAttribute("message", new Message("Contact deleted successfully...", "success"));
		return "redirect:/user/show-contacts/0";
	}
	
	
	//update controller
	@PostMapping("/update-contact/{cId}")
	public String UpdateForm(
			@PathVariable("cId") Integer cId,
			Model model) {
		model.addAttribute("title","Update Contact");
		
		Optional<Contacts> contacts = this.contactRepository.findById(cId);
		model.addAttribute("contacts",contacts);
		return "normal/update_form";
	}
	
	
	//update contact handler
	@RequestMapping(value = "/process-update",method = RequestMethod.POST)
	public String updateHandler(
			@ModelAttribute Contacts contacts,
			@RequestParam("profileImage") MultipartFile multipartFile,
			Model model,
			HttpSession session,
			Principal principal) {
			
			Contacts oldcontactDetail = this.contactRepository.findById(contacts.getcId()).get();
			
			try {
				if(multipartFile.isEmpty()) {
					//if new file is empty save the previous image file as new
					contacts.setContactImage(oldcontactDetail.getContactImage());
				}else {
					//if new image file has been selected/updated 
					//then we first need to delete previous image file
					File deleteFile=new ClassPathResource("static/images").getFile();
					File file2=new File(deleteFile, oldcontactDetail.getContactImage());
					file2.delete();
					
					//save/update the new file
					File saveFile = new ClassPathResource("static/images").getFile();
					Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+multipartFile.getOriginalFilename());
					Files.copy(multipartFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
					contacts.setContactImage(multipartFile.getOriginalFilename());
				}
				User user=this.userRepository.getUserByUserName(principal.getName());
				contacts.setUser(user);
				this.contactRepository.save(contacts); 
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			return "redirect:/User/contact/"+contacts.getcId();
	}
	
	//show profile for logged in user
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title","Profile");
		
		return "normal/profile";
	}
}
