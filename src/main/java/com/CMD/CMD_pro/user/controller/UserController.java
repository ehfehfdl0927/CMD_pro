package com.CMD.CMD_pro.user.controller;

import com.CMD.CMD_pro.board.controller.WriteForm;
import com.CMD.CMD_pro.board.domain.BoardVO;
import com.CMD.CMD_pro.board.domain.FileVO;
import com.CMD.CMD_pro.user.domain.UserVO;
import com.CMD.CMD_pro.user.mapper.UserMapper;
import org.apache.catalina.User;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

@Controller
public class UserController {

    private final UserMapper userMapper;

    @Autowired
    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Autowired
    private JavaMailSender sender;

    @GetMapping("/join")
    public String join(Model model, HttpSession session) throws Exception{
        String filename;
        String userID;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);
        return "join";
    }

    @PostMapping("/JoinAction")
    public String joinAction(JoinForm form) throws Exception{
        UserVO user = new UserVO();
        user.setUser_id(form.getUser_id());
        user.setUser_pwd(form.getUser_pwd());
        user.setUser_name(form.getUser_name());
        user.setUser_email(form.getUser_email());

        if(form.getUser_age().equals("")){
            user.setUser_age(0);
        } else{
            user.setUser_age(Integer.parseInt(form.getUser_age()));
        }

        user.setUser_major(form.getUser_major());
        user.setUser_gender(form.getUser_gender());
        userMapper.userJoin(user);
        return "redirect:/main";
    }

    @ResponseBody
    @RequestMapping(value = "/idChk", method = RequestMethod.POST)
    public int idChk(HttpServletRequest req) throws Exception{
        String user_id = req.getParameter("id");
        if(user_id.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9].*")){
            int result = userMapper.idChk(user_id);
            return result;
        }else {
            return -1;
        }
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) throws Exception{
        String filename;
        String userID;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);
        return "login";
    }

    @PostMapping("/LoginAction")
    public String loginAction(Model model, JoinForm form, HttpServletRequest request)throws Exception {
        String userID = form.getUser_id();
        String userPassword = form.getUser_pwd();
        UserVO user = userMapper.userLogin(userID);
        if(user != null){
            if(user.getUser_pwd().equals(userPassword)){
                HttpSession session = request.getSession();
                session.setAttribute("id",userID);
                String filename;
                if((String) session.getAttribute("id") != null){
                    userID = (String) session.getAttribute("id");
                    if(user.getUser_profile() == null){
                        filename = "non";
                    }
                    else{
                        filename = user.getUser_profile();
                    }
                } else {
                    filename = "non";
                }

                model.addAttribute("filename",filename);
                return "cmdev";
            }
            model.addAttribute("msg","비밀번호가 틀립니다.");
            model.addAttribute("url","login");
            return "alert";
        }

        model.addAttribute("msg","존재하지 않는 아이디입니다.");
        model.addAttribute("url","login");
        return "alert";
    }

    @GetMapping("/logoutAction")
    public String LogoutAction(HttpSession session, Model model) throws Exception{
        session.invalidate();

        String filename = "non";

        model.addAttribute("filename",filename);
        return "cmdev";
    }

    @GetMapping("/userUpdateCheck")
    public String userUpdateCheck(Model model, HttpSession session) throws Exception{
        String filename;
        String userID;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);
        return "userUpdateCheck";
    }

    @PostMapping("/userUpdate")
    public String userUpdate(HttpSession session,JoinForm form,Model model) throws Exception {
        if(session.getAttribute("id") == null){
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","main");
            return "alert";
        }
        String userID = (String) session.getAttribute("id");
        String userPassword = form.getUser_pwd();
        UserVO user = userMapper.userLogin(userID);
        String filename = user.getUser_profile();
        if (user.getUser_pwd().equals(userPassword)) {
            model.addAttribute("filename",filename);
            model.addAttribute("user",user);
            return "userUpdate";
        }
        else {
            model.addAttribute("msg","비밀번호가 일치하지 않습니다.");
            model.addAttribute("url","userUpdateCheck");
            return "alert";
        }
    }

    @PostMapping("/userUpdateAction")
    public String userUpdateAction(Model model,JoinForm form,HttpSession session,@RequestPart MultipartFile files) throws Exception{
        UserVO user = new UserVO();
        user.setUser_id(form.getUser_id());
        user.setUser_pwd(form.getUser_pwd());
        user.setUser_name(form.getUser_name());
        user.setUser_age(Integer.parseInt(form.getUser_age()));
        user.setUser_major(form.getUser_major());
        user.setUser_email(form.getUser_email());
        user.setUser_gender(form.getUser_gender());
        userMapper.userUpdate(user);
        if(form.getModify().equals("Y")){
            if(form.getDefault_img().equals("Y")){
                String userID = (String) session.getAttribute("id");
                userMapper.userProfile("default.png", userID);
            }
            else {
                String userID = (String) session.getAttribute("id");
                String fileName = files.getOriginalFilename();
                File destinationFile;
                String destinationFileName;
                String fileUrl = "C:\\images\\";
                String fileNameExtension = FilenameUtils.getExtension(fileName).toLowerCase();
                if(fileNameExtension.equals("jpg") || fileNameExtension.equals("png") || fileNameExtension.equals("jpeg")){
                    do {
                        destinationFileName = RandomStringUtils.randomAlphanumeric(32) + "." + fileNameExtension;
                        destinationFile = new File(fileUrl + destinationFileName);

                    } while (destinationFile.exists());
                    destinationFile.getParentFile().mkdirs();
                    files.transferTo(destinationFile);
                    userMapper.userProfile(destinationFileName, userID);
                    model.addAttribute("msg","회원정보가 수정되었습니다.");
                    model.addAttribute("url","main");
                    return "alert";
                } else{
                    model.addAttribute("msg","이미지 파일만 업로드 가능합니다.");
                    model.addAttribute("url","back");
                    return "alert";
                }

            }
        }
        model.addAttribute("msg","회원정보가 수정되었습니다.");
        model.addAttribute("url","main");
        return "alert";

    }

    @GetMapping("/display")
    public ResponseEntity<Resource> display(@Param("filename") String filename){
        String path = "C:\\images\\";
        Resource resource = new FileSystemResource(path + filename);
        HttpHeaders header = new HttpHeaders();
        Path filePath = null;
        try{
            filePath = Paths.get(path + filename);
            header.add("Content-Type", Files.probeContentType(filePath));
        } catch (IOException e){
            e.printStackTrace();
        }
        return new ResponseEntity<Resource>(resource,header, HttpStatus.OK);
    }

    @GetMapping("/profile")
    public String getProfile(){ return "profile";}

    @PostMapping("/profile")   //게시물 리스트 가져오기 post방식 글쓰기 완료후 폼 액션
    public String WriteAction(WriteForm form, RedirectAttributes redirect, HttpSession session, Model model, @RequestPart MultipartFile files) throws Exception{
        String userID = (String) session.getAttribute("id");
        String fileName = files.getOriginalFilename();
        String fileNameExtension = FilenameUtils.getExtension(fileName).toLowerCase();
        File destinationFile;
        String destinationFileName;
        String fileUrl = "C:\\images\\";
        do{
            destinationFileName = RandomStringUtils.randomAlphanumeric(32) + "." + fileNameExtension;
            destinationFile = new File(fileUrl + destinationFileName);

        } while (destinationFile.exists());
        destinationFile.getParentFile().mkdirs();
        files.transferTo(destinationFile);
        userMapper.userProfile(destinationFileName,userID);

        return "redirect:/main";
    }

    @GetMapping("/findPassword")
    public String findPassword(Model model, HttpSession session) throws Exception{
        String filename;
        String userID;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);
        return "findPassword";
    }

    @PostMapping("/sendEmail")
    public String sendEmail(JoinForm form, Model model,HttpSession session) throws Exception{
        String id = form.getUser_id();
        UserVO user = userMapper.userLogin(id);

        if(user == null){
            model.addAttribute("msg","존재하지 않는 아이디입니다.");
            model.addAttribute("url","findPassword");
            return "alert";
        }

        String email = user.getUser_email();
        Random r = new Random();
        int num = r.nextInt(999999);

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            helper.setTo(email);
            helper.setSubject("CMD 비밀번호 찾기 인증코드");
            helper.setText("회원님의 인증코드는"+num+"입니다.");
            helper.setFrom("kimjoohe3@gmail.com","CMD");
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        sender.send(message);

        String filename;
        String userID;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user2 = userMapper.userLogin(userID);
            filename = user2.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);

        model.addAttribute("code",num);
        model.addAttribute("id",id);
        return "codeCheck";
    }


    @PostMapping("/PasswordCheck")
    public String passwordCheck(JoinForm form, Model model,HttpSession session) throws Exception{
        String userCode = form.getUser_code();
        String correctCode = form.getCorrect_code();
        String userId = form.getUser_id();
        UserVO user = userMapper.userLogin(userId);
        String userPwd = user.getUser_pwd();
        if(userCode.equals(correctCode)){
            String filename;
            String userID;
            if((String) session.getAttribute("id") != null){
                userID = (String) session.getAttribute("id");
                UserVO user2 = userMapper.userLogin(userID);
                filename = user2.getUser_profile();

            } else {
                filename = "non";
            }
            model.addAttribute("filename",filename);
            model.addAttribute("user_pwd",userPwd);
            return "passwordCheck";
        }
        else{
            model.addAttribute("msg","인증코드가 틀립니다.");
            model.addAttribute("url","findPassword");
            return "alert";
        }
    }

    @GetMapping("/userWithdrawal")
    public String userWithdrawal(HttpSession session, Model model,@RequestParam("id") String id) throws Exception{
        session.invalidate();
        userMapper.userWithdrawal(id);
        userMapper.userProfile("default.png",id);
        String filename = "non";

        model.addAttribute("filename",filename);
        return "cmdev";
    }

    @GetMapping("/withdrawalCheck")
    public String withdrawalCheck(HttpSession session, Model model) throws Exception{
        String filename;
        String userID;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();
            model.addAttribute("filename",filename);
            return "withdrawalCheck";

        } else {
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","login");
            return "alert";
        }
    }

    @PostMapping("/userWithdrawal")
    public String postUserWithdrawal(HttpSession session, JoinForm form, Model model) throws Exception{
        String pwd = form.getUser_pwd();
        String id = (String) session.getAttribute("id");
        UserVO user = userMapper.userLogin(id);
        if(pwd.equals(user.getUser_pwd())){
            session.invalidate();
            userMapper.userWithdrawal(id);
            userMapper.userProfile("default.png",id);
            model.addAttribute("msg","회원탈퇴가 완료되었습니다.");
            model.addAttribute("url","main");
            return "alert";
        } else{
            model.addAttribute("msg","비밀번호가 일치하지 않습니다.");
            model.addAttribute("url","withdrawalCheck");
            return "alert";
        }
    }




}
