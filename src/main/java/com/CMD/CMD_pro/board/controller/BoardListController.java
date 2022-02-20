package com.CMD.CMD_pro.board.controller;


import com.CMD.CMD_pro.board.domain.BoardVO;
import com.CMD.CMD_pro.board.domain.CommentVO;
import com.CMD.CMD_pro.board.domain.FileVO;
import com.CMD.CMD_pro.board.domain.SearchVO;
import com.CMD.CMD_pro.board.mapper.BoardMapper;
import com.CMD.CMD_pro.chat.domain.ChatVO;
import com.CMD.CMD_pro.user.domain.UserVO;
import com.CMD.CMD_pro.user.mapper.UserMapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Controller
public class BoardListController {

    private final BoardMapper boardMapper;
    private final  UserMapper userMapper;

    @Autowired
    public BoardListController(BoardMapper boardMapper, UserMapper userMapper) {
        this.boardMapper = boardMapper;
        this.userMapper = userMapper;
    }



    @GetMapping("/board")  //게시물 리스트 가져오기 get방식
    public String list(HttpServletRequest request, Model model, @RequestParam("kind") String kind, @RequestParam("realm") String realm, HttpSession session) throws Exception{
        String pageNumber = "1";
        String filename = null;
        int off = 0;
        int target2 = 0; //새로
        if(request.getParameter("pageNumber") != null){
            pageNumber = request.getParameter("pageNumber");
        }
        if(Integer.parseInt(pageNumber) == 1){
            off = 0;
        }
        else{
            off = (Integer.parseInt(pageNumber) - 1) * 20;
        }
        List<BoardVO> board = boardMapper.boardList(off, kind, realm);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < board.size(); i++){
            Date reg_date = board.get(i).getReg_date();
            String real_date = format.format(reg_date);
            board.get(i).setReal_date(real_date);
        }

        int target =(Integer.parseInt(pageNumber) - 1) * 10;
        int target1 = boardMapper.targetPage(target);
        target1 = target1 / 10;
        int startPage = (Integer.parseInt(pageNumber) / 10) * 10 + 1;
        if(Integer.parseInt(pageNumber) % 10 == 0) startPage -= 10;
        int page = Integer.parseInt(pageNumber);
        int count = boardMapper.boardCount(kind,realm);
        if((count - (page * 20)) % 10 != 0 && count - (page * 20) > 0 ){ //새로 매앞에꺼만 20고침
            target2 = ((count - (page * 20)) / 20) + 1; //고침
        } else if((count - (page * 20)) % 10 == 0 && count - (page * 20) > 0) { //새로
            if((count -(page * 20)) % 20 > 1){
                target2 = ((count - (page * 20)) / 20) + 1;
            }
            else {
                target2 = ((count - (page * 20)) / 20); //고침
            }
        } else {
            target2 = 0;
        }
        int resultCount = count - (page * 20); //현재페이지 이후부터의 남은 게시물 갯수
        List<BoardVO> hotTopic = boardMapper.HotTopicList(kind,realm);
        List<String> hotContentList = new ArrayList<String>();
        for(int i=0; i < hotTopic.size(); i ++){
            String hotContent = hotTopic.get(i).getContent();
            if(hotContent.length() > 55){
                hotContent = hotContent.substring(0,55);
            }
            hotContentList.add(hotContent);
        }
        if(session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            if (user.getUser_profile() == null) {
                filename = "non";
            } else {
                filename = user.getUser_profile();
            }
        }
        model.addAttribute("filename", filename);
        model.addAttribute("board", board); //게시물 메퍼에 boardList 로 얻은 결과를 리스트로 담아서 boardList.html로 전달
        model.addAttribute("target", target1);
        model.addAttribute("startPage", startPage);
        model.addAttribute("page", page);
        model.addAttribute("count", resultCount);
        model.addAttribute("target2", target2);
        model.addAttribute("kind",kind); //새로
        model.addAttribute("realm",realm);//새로
        if(hotTopic.size() >= 1) model.addAttribute("hotTopic1",hotTopic.get(0));
        else model.addAttribute("hotTopic1","non");
        if(hotTopic.size() >= 2) model.addAttribute("hotTopic2",hotTopic.get(1));
        else model.addAttribute("hotTopic2","non");
        if(hotTopic.size() >= 3) model.addAttribute("hotTopic3",hotTopic.get(2));
        else model.addAttribute("hotTopic3","non");
        if(hotTopic.size() >= 4) model.addAttribute("hotTopic4",hotTopic.get(3));
        else model.addAttribute("hotTopic4","non");
        if(hotTopic.size() >= 5) model.addAttribute("hotTopic5",hotTopic.get(4));
        else model.addAttribute("hotTopic5","non");
        if(hotContentList.size() >=1) model.addAttribute("hotContent1", hotContentList.get(0));
        if(hotContentList.size() >=2) model.addAttribute("hotContent2", hotContentList.get(1));
        if(hotContentList.size() >=3) model.addAttribute("hotContent3", hotContentList.get(2));
        if(hotContentList.size() >=4) model.addAttribute("hotContent4", hotContentList.get(3));
        if(hotContentList.size() >=5) model.addAttribute("hotContent5", hotContentList.get(4));
        return "cmd_community_boardList";
    }

    @PostMapping("board")   //게시물 리스트 가져오기 post방식 글쓰기 완료후 폼 액션
    public String WriteAction(WriteForm form, RedirectAttributes redirect, HttpSession session, Model model,
                              @RequestPart MultipartFile files, HttpServletRequest request) throws Exception{
        BoardVO board = new BoardVO();
        board.setWriter(form.getWriter());  //BoardVO형 도메인 모델에 폼 데이터 담기
        board.setSubject(form.getSubject());
        board.setContent(form.getContent());
        String notice = form.getNotice();
        int board_bno = form.getBno();
        String userID = (String) session.getAttribute("id");
        UserVO user = userMapper.userLogin(userID); //로그인한 회원 아이디세션값으로 권한확인
        if(notice == null){
            board.setNotice(0);
        } else{
            if(user.getUser_manager()==1){
                board.setNotice(Integer.parseInt(notice));
            }
            else{
              model.addAttribute("msg","관리자 권한이 없습니다.");
              model.addAttribute("url","board?kind="+form.getKind()+"&realm="+form.getRealm());
              return "alert";
            }
        }
        board.setKind(form.getKind());
        board.setRealm(form.getRealm());

        boardMapper.boardInsert(board); //사용자 작성글 db에 추가

            String fileName = files.getOriginalFilename();
            String fileNameExtension = FilenameUtils.getExtension(fileName).toLowerCase();
            File destinationFile;
            String destinationFileName;
            String fileUrl = "C:\\test1\\";
            do{ //파일 업로드
            destinationFileName = RandomStringUtils.randomAlphanumeric(32) + "." + fileNameExtension;
            destinationFile = new File(fileUrl + destinationFileName);

            } while (destinationFile.exists());
            destinationFile.getParentFile().mkdirs();
            files.transferTo(destinationFile);
        FileVO file = new FileVO();
        file.setBno(board_bno);
        file.setFilename(destinationFileName);
        file.setFileoriginname(fileName);
        file.setFileurl(fileUrl);
        boardMapper.fileInsert(file); //파일db에 사용자가 업로드한 파일등록

        redirect.addAttribute("kind", form.getKind()); //kind 라는 파라미터name으로 입력 폼값전달
        redirect.addAttribute("realm", form.getRealm()); //realm 라는 파라미터name으로 입력 폼값전달
        return "redirect:/board"; //GetMapping의 ("/board") URL 매핑되있는 함수 실행
    }
    @GetMapping("/write")                  //글쓰기
    public String write(@RequestParam("kind") String kind, @RequestParam("realm") String realm, Model model, HttpSession session) throws Exception{
        String filename;
        String id = (String) session.getAttribute("id");
        if(id == null){
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","login"); //메세지와 url을 모델로 담아 알림창을 띄울 alert.html로 전달
            return "alert";
        }
        UserVO user = userMapper.userLogin(id);
        filename = user.getUser_profile();
        int maxBno = boardMapper.selectMax() + 1;
        model.addAttribute("user",user);
        model.addAttribute("kind", kind);
        model.addAttribute("realm",realm);
        model.addAttribute("maxBno", maxBno);
        model.addAttribute("filename",filename);
        return "write";
    }

    @GetMapping("/view")    //글 상세보기
    public String BoardView(@RequestParam("bno") int bno, @RequestParam("kind") String kind, @RequestParam("realm") String realm, Model model, HttpSession session,HttpServletRequest request) throws Exception{
        String userID = (String)session.getAttribute("id");
        String filename;
        String pageNumber = null;
        String option = "non";
        String search = "non";
        String search_option = "non";
        String keyword = "non";
        String mainSearch = "non";
        if(request.getParameter("search_option") != null){
            search_option = request.getParameter("search_option");
        }
        if(request.getParameter("keyword") != null){
            keyword = request.getParameter("keyword");
        }
        if(request.getParameter("search") != null){
            search = request.getParameter("search");
        }
        if(request.getParameter("option") != null){
            option = request.getParameter("option");
        }
        if(request.getParameter("mainSearch") != null){
            mainSearch = request.getParameter("mainSearch");
        }
        pageNumber = request.getParameter("pageNumber");
        if(userID == null){
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","login"); //메세지와 url을 모델로 담아 알림창을 띄울 alert.html로 전달
            return "alert";
        }
        boardMapper.boardHit(bno);
        BoardVO board = boardMapper.boardView(bno,kind,realm);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date reg_date = board.getReg_date();
        String real_date = format.format(reg_date);
        board.setReal_date(real_date);


        int recommendCheck = boardMapper.recommendCheck(kind,realm,bno,userID);
        UserVO user = userMapper.userLogin(userID);
        model.addAttribute("user",user);
        model.addAttribute("recommendCheck",recommendCheck);
        model.addAttribute("board", board); //저장된 board 객체를 모델로 담아 view.html로 전달
        if(boardMapper.fileDetail(bno) != null){
            model.addAttribute("file", boardMapper.fileDetail(bno));
            model.addAttribute("fileExistence","yes");
        }
        else {
            FileVO file = new FileVO();
            file.setFileoriginname("no");
            model.addAttribute("file",file);
            model.addAttribute("fileExistence","no");
        }

        List<CommentVO> commentList = boardMapper.CommentList(bno);

        for(int i = 0; i < commentList.size(); i++){
            Date date = commentList.get(i).getReg_date();
            String real_date2 = format.format(date);
            commentList.get(i).setReal_date(real_date2);
        }

        int commentCount = boardMapper.CommentCount(bno);
        filename = user.getUser_profile();
        model.addAttribute("commentCount",commentCount);
        model.addAttribute("commentList",commentList);
        model.addAttribute("page",pageNumber);
        model.addAttribute("kind",kind);
        model.addAttribute("realm",realm);
        model.addAttribute("option",option);
        model.addAttribute("search",search);
        model.addAttribute("search_option",search_option);
        model.addAttribute("keyword",keyword);
        model.addAttribute("mainSearch",mainSearch);
        model.addAttribute("filename",filename);


        return "view";

    }

    @PostMapping("/update")    //글 수정
    public String Update(UpdateForm form, Model model, HttpSession session) throws Exception{
        String option = form.getOption();
        String search = form.getSearch();
        String search_option = form.getSearch_option();
        String keyword = form.getKeyword();
        String mainSearch = form.getMainSearch();
        String page = form.getPage();
        String userID = (String)session.getAttribute("id");
        String filename = null;
        if(!userID.equals(form.getWriter())){
            model.addAttribute("msg","접근할 수 없습니다.");
            model.addAttribute("url","board?kind="+form.getKind()+"&realm="+form.getRealm());
            return "alert";
        }
        BoardVO board = new BoardVO();
        board.setBno(form.getBno()); //BoardVO형 도메인 모델에 폼 데이터 담기
        board.setSubject(form.getSubject());
        board.setContent(form.getContent());
        board.setKind(form.getKind());
        board.setRealm(form.getRealm());
        filename = form.getFilename();
        if(form.getFilename()==""){
            filename = "no";
        }
        UserVO user = userMapper.userLogin(userID);
        String profileName = user.getUser_profile();
        model.addAttribute("board",board); //저장된 board 객체를 모델로 담아 update.html로 전달
        model.addAttribute("filename",filename);
        model.addAttribute("option",option);
        model.addAttribute("search",search);
        model.addAttribute("search_option",search_option);
        model.addAttribute("keyword",keyword);
        model.addAttribute("mainSearch",mainSearch);
        model.addAttribute("page",page);
        model.addAttribute("profile_name", profileName);
        return "update";
    }

    @PostMapping("/UpdateAction")    //글 수정 폼 액션
    public String UpdateAction(UpdateForm form, RedirectAttributes redirect, @RequestPart MultipartFile files) throws Exception{
        BoardVO board = new BoardVO();
        board.setBno(form.getBno()); //BoardVO형 도메인 모델에 폼 데이터 담기
        board.setSubject(form.getSubject());
        board.setContent(form.getContent());
        board.setKind(form.getKind());
        board.setRealm(form.getRealm());
        boardMapper.boardUpdate(board); //저장된 board 객체를 파라미터로 담아 게시물 메퍼에 boardUpdate 메소드 실행

        int bno = form.getBno();
        String fileName = files.getOriginalFilename();
        String fileNameExtension = FilenameUtils.getExtension(fileName).toLowerCase();
        File destinationFile;
        String destinationFileName;
        String fileUrl = "C:\\test1\\";
        do {
            destinationFileName = RandomStringUtils.randomAlphanumeric(32) + "." + fileNameExtension;
            destinationFile = new File(fileUrl + destinationFileName);

        } while (destinationFile.exists());
        destinationFile.getParentFile().mkdirs();
        files.transferTo(destinationFile);
        FileVO file = new FileVO();
        file.setBno(bno);
        file.setFilename(destinationFileName);
        file.setFileoriginname(fileName);
        file.setFileurl(fileUrl);
        if(boardMapper.fileDetail(bno) != null) {
            boardMapper.fileDelete(bno);
        }
        boardMapper.fileUpdate(file);

        if(form.getOption() != "non"){
            redirect.addAttribute("option",form.getOption());
        }
        if(form.getSearch() != "non"){
            redirect.addAttribute("search",form.getSearch());
        }
        if(form.getSearch_option() != "non"){
            redirect.addAttribute("search_option",form.getSearch_option());
        }
        if(form.getKeyword() != "non"){
            redirect.addAttribute("keyword",form.getKeyword());
        }
        if(form.getMainSearch() != "non"){
            redirect.addAttribute("mainSearch",form.getMainSearch());
        }

        redirect.addAttribute("kind", form.getKind());
        redirect.addAttribute("realm", form.getRealm());
        redirect.addAttribute("bno",form.getBno());
        redirect.addAttribute("pageNumber",form.getPage());
        return "redirect:/view"; //("/board")로 매핑된 get메소드 실행
    }


    @GetMapping("/delete")  //글 삭제
    public String BoardDelete(@RequestParam("bno") int bno, @RequestParam("kind") String kind, @RequestParam("realm") String realm, @RequestParam("writer") String writer, Model model, HttpSession session) throws Exception{
        String userID = (String)session.getAttribute("id");
        UserVO user = userMapper.userLogin(userID);
        if(!userID.equals(writer) && user.getUser_manager()==0){
            model.addAttribute("msg","접근할 수 없습니다.");
            model.addAttribute("url","board?kind="+kind+"&realm="+realm);
            return "alert";
        }
        boardMapper.boardDelete(bno,kind,realm,writer); //파라미터로 받은 bno를 담아 게시물 메퍼의 boardDelete메소드 실행
        boardMapper.CommentSetDelete(bno);
        model.addAttribute("msg","글을 삭제하였습니다.");
        model.addAttribute("url","board?kind="+kind+"&realm="+realm); //메세지와 url을 모델로 담아 알림창을 띄울 alert.html로 전달
        return "alert";

    }

    @PostMapping("/search_list")  //글 검색
    public String BoardSearch(HttpServletRequest request, SearchForm form, Model model, HttpSession session) throws Exception{
        String search_option = form.getSearch_option(); //String형 변수에 폼 데이터 저장
        String keyword = form.getKeyword();
        String kind = form.getKind();
        String realm = form.getRealm();
        String pageNumber = "1";
        String filename = null;
        int off = 0;
        int target2 = 0; //새로
        if(request.getParameter("pageNumber") != null){
            pageNumber = request.getParameter("pageNumber");
        }
        if(Integer.parseInt(pageNumber) == 1){
            off = 0;
        }
        else{
            off = (Integer.parseInt(pageNumber) - 1) * 20;
        }
        int startPage = (Integer.parseInt(pageNumber) / 10) * 10 + 1;
        if(Integer.parseInt(pageNumber) % 10 == 0) startPage -= 10;
        int page = Integer.parseInt(pageNumber);
        int count = boardMapper.searchCount(search_option,keyword,kind,realm);
        if((count - (page * 20)) % 10 != 0 && count - (page * 20) > 0 ){ //새로 매앞에꺼만 20고침
            target2 = ((count - (page * 20)) / 20) + 1; //고침
        } else if((count - (page * 20)) % 10 == 0 && count - (page * 20) > 0) { //새로
            if((count -(page * 20)) % 20 > 1){
                target2 = ((count - (page * 20)) / 20) + 1;
            }
            else {
                target2 = ((count - (page * 20)) / 20); //고침
            }
        } else {
            target2 = 0;
        }
        int resultCount = count - (page * 20); //현재페이지 이후부터의 남은 게시물 갯수
        List<BoardVO> board = boardMapper.boardSearch(search_option, keyword,kind,realm,off);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < board.size(); i++){
            Date reg_date = board.get(i).getReg_date();
            String real_date = format.format(reg_date);
            board.get(i).setReal_date(real_date);
        }

        if(session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            if (user.getUser_profile() == null) {
                filename = "non";
            } else {
                filename = user.getUser_profile();
            }
        }
        model.addAttribute("filename", filename);
        model.addAttribute("board",board); //반환된 결과인 board를 모델로 담아 search.html로 전달
        model.addAttribute("keyword",keyword); //키워드 전달
        model.addAttribute("search_option",search_option); //옵션 전달
        model.addAttribute("kind",kind);
        model.addAttribute("realm",realm);
        model.addAttribute("startPage",startPage);
        model.addAttribute("page",page);
        model.addAttribute("count",resultCount);
        model.addAttribute("target2",target2);
        return "cmd_search";
    }

    @GetMapping("/main")
    public String MainPage(HttpSession session, Model model) throws Exception{
        String userID;
        String filename;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
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

    @GetMapping("/search_list")  //글 검색
    public String BoardSearch2(@RequestParam("search_option") String search_option, @RequestParam("keyword") String keyword, @RequestParam("kind") String kind, @RequestParam("realm") String realm, HttpServletRequest request, SearchForm form, Model model, HttpSession session) throws Exception{
        String pageNumber = "1";
        int off = 0;
        int target2 = 0; //새로
        String filename = null;
        if(request.getParameter("pageNumber") != null){
            pageNumber = request.getParameter("pageNumber");
        }
        if(Integer.parseInt(pageNumber) == 1){
            off = 0;
        }
        else{
            off = (Integer.parseInt(pageNumber) - 1) * 20;
        }
        int startPage = (Integer.parseInt(pageNumber) / 10) * 10 + 1;
        if(Integer.parseInt(pageNumber) % 10 == 0) startPage -= 10;
        int page = Integer.parseInt(pageNumber);
        int count = boardMapper.searchCount(search_option,keyword,kind,realm);
        if((count - (page * 20)) % 10 != 0 && count - (page * 20) > 0 ){ //새로 매앞에꺼만 20고침
            target2 = ((count - (page * 20)) / 20) + 1; //고침
        } else if((count - (page * 20)) % 10 == 0 && count - (page * 20) > 0) { //새로
            if((count -(page * 20)) % 20 > 1){
                target2 = ((count - (page * 20)) / 20) + 1;
            }
            else {
                target2 = ((count - (page * 20)) / 20); //고침
            }
        } else {
            target2 = 0;
        }
        int resultCount = count - (page * 20); //현재페이지 이후부터의 남은 게시물 갯수
        List<BoardVO> board = boardMapper.boardSearch(search_option, keyword,kind,realm,off); //저장된 변수들을 파라미터로 담아 boardSearch 실행후 결과를 board에 저장
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < board.size(); i++){
            Date reg_date = board.get(i).getReg_date();
            String real_date = format.format(reg_date);
            board.get(i).setReal_date(real_date);
        }
        if(session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            if (user.getUser_profile() == null) {
                filename = "non";
            } else {
                filename = user.getUser_profile();
            }
        }
        model.addAttribute("filename", filename);
        model.addAttribute("board",board); //반환된 결과인 board를 모델로 담아 search.html로 전달
        model.addAttribute("keyword",keyword); //키워드 전달
        model.addAttribute("search_option",search_option); //옵션 전달
        model.addAttribute("kind",kind);
        model.addAttribute("realm",realm);
        model.addAttribute("startPage",startPage);
        model.addAttribute("page",page);
        model.addAttribute("count",resultCount);
        model.addAttribute("target2",target2);
        return "cmd_search";
    }

    @ResponseBody
    @RequestMapping(value = "/recommend", method = RequestMethod.POST)
    public int recommend(HttpServletRequest req) throws Exception{
        String kind = req.getParameter("kind");
        String realm = req.getParameter("realm");
        String userID = req.getParameter("id");
        int bno = Integer.parseInt(req.getParameter("bno"));
        BoardVO board = boardMapper.boardView(bno,kind,realm);
        int recommend = board.getRecommend();
        int check = boardMapper.recommendCheck(kind,realm,bno,userID);
        if(check == 0){
            boardMapper.recommendAdd(bno);
            boardMapper.recommendInsert(kind,realm,bno,userID);
            return recommend + 1;
        } else {
            boardMapper.recommendDelete(kind,realm,bno,userID);
            boardMapper.recommendSubtract(bno);
            return recommend - 1;
        }
    }

    @GetMapping("/fileDown")
    public void fileDown(@RequestParam("bno") int bno,HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException, Exception{
        request.setCharacterEncoding("UTF-8");
        FileVO fileVO = boardMapper.fileDetail(bno);
        try {
            String fileUrl = fileVO.getFileurl();
            System.out.println(fileUrl);
            fileUrl += "/";
            String savePath = fileUrl;
            String fileName = fileVO.getFilename();

            //실제 내보낼 파일명
            String originFileName = fileVO.getFileoriginname();
            InputStream in = null;
            OutputStream os = null;
            File file= null;
            Boolean skip = false;
            String client = "";

            //파일을 읽어 스트림에 담기
            try {
                file = new File(savePath, fileName);
                in = new FileInputStream(file);
            } catch (FileNotFoundException fe) {
                skip = true;
            }

            client = request.getHeader("User-Agent");

            //파일 다운로드 헤더 지정
            response.reset();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Description", "HTML Generated Data");

            if(!skip) {
                //IE
                if(client.indexOf("MSIE") != -1) {
                    response.setHeader("Content-Disposition", "attachment; filename=\""
                            + java.net.URLEncoder.encode(originFileName, "UTF-8").replaceAll("\\+", "\\ ") + "\"");
                    //IE 11 이상
                } else if (client.indexOf("Trident") != -1) {
                    response.setHeader("Content-Disposition", "attachment; filename=\""
                            + java.net.URLEncoder.encode(originFileName, "UTF-8").replaceAll("\\+", "\\ ") + "\"");
                    //한글 파일명 처리
                } else {
                    response.setHeader("Content-Disposition", "attachment; filename=\"" +
                            new String(originFileName.getBytes("UTF-8"), "ISO8859_1") + "\"");
                    response.setHeader("Content-Type", "application/octet-stream; charset=utf-8");
                }

                response.setHeader("Content-Length", ""+file.length());
                os = response.getOutputStream();
                byte b[] = new byte[(int) file.length()];
                int leng = 0;

                while ((leng = in.read(b)) > 0) {
                    os.write(b, 0, leng);
                }
            } else {
                response.setContentType("text/html; charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.println("<script> alert('파일을 찾을 수 없습니다.'); history.back(); </script>");
                out.flush();
            }

            in.close();
            os.close();

        } catch (Exception e) {
            System.out.println("ERROR : " + e.getStackTrace());
        }

    }

    @ResponseBody
    @RequestMapping(value = "/fileDelete", method = RequestMethod.POST)
    public void fileDelete(HttpServletRequest req) throws Exception{
        int bno = Integer.parseInt(req.getParameter("bno"));
        boardMapper.fileDelete(bno);
    }

    @PostMapping("/commentInsert") //댓글등록
    public String commentInsert(CommentForm form, RedirectAttributes redirect) throws Exception{
        String pageNumber = null;
        pageNumber = form.getPageNumber();
        CommentVO comment = new CommentVO();
        comment.setBno(form.getBoard_bno());
        comment.setWriter(form.getComment_writer());
        comment.setContent(form.getCommentContent());
        boardMapper.CommentInsert(comment);
        if(form.getOption() != "non"){
            redirect.addAttribute("option",form.getOption());
        }
        if(form.getSearch() != "non"){
            redirect.addAttribute("search",form.getSearch());
        }
        if(form.getSearch_option() != "non"){
            redirect.addAttribute("search_option",form.getSearch_option());
        }
        if(form.getKeyword() != "non"){
            redirect.addAttribute("keyword",form.getKeyword());
        }
        if(form.getMainSearch() != "non"){
            redirect.addAttribute("mainSearch",form.getMainSearch());
        }

        redirect.addAttribute("bno",form.getBoard_bno());
        redirect.addAttribute("kind",form.getBoard_kind());
        redirect.addAttribute("realm",form.getBoard_realm());
        redirect.addAttribute("pageNumber",pageNumber);

       return "redirect:/view";
    }


    @PostMapping("/replyInsert") //대댓글 등록
    public String replyInsert(CommentForm form, RedirectAttributes redirect) throws Exception{
        String pageNumber = null;
        pageNumber = form.getPageNumber();
        CommentVO comment = new CommentVO();
        comment.setBno(form.getBoard_bno());
        comment.setC_sequence(form.getC_sequence());
        comment.setWriter(form.getComment_writer());
        comment.setContent(form.getCommentContent());
        boardMapper.ReplyInsert(comment);
        if(form.getOption() != "non"){
            redirect.addAttribute("option",form.getOption());
        }
        if(form.getSearch() != "non"){
            redirect.addAttribute("search",form.getSearch());
        }
        if(form.getSearch_option() != "non"){
            redirect.addAttribute("search_option",form.getSearch_option());
        }
        if(form.getKeyword() != "non"){
            redirect.addAttribute("keyword",form.getKeyword());
        }
        if(form.getMainSearch() != "non"){
            redirect.addAttribute("mainSearch",form.getMainSearch());
        }
        redirect.addAttribute("bno",form.getBoard_bno());
        redirect.addAttribute("kind",form.getBoard_kind());
        redirect.addAttribute("realm",form.getBoard_realm());
        redirect.addAttribute("pageNumber",pageNumber);
        return "redirect:/view";

    }


    @PostMapping("/commentUpdate") //댓글 대댓글수정
    public String commentUpdate(CommentForm form, RedirectAttributes redirect) throws Exception{
        String pageNumber = null;
        pageNumber = form.getPageNumber();
        int commentBno = 0;
        String commentContent = null;
        commentBno = form.getC_bno();
        commentContent = form.getCommentContent();
        boardMapper.CommentUpdate(commentBno,commentContent);
        if(form.getOption() != "non"){
            redirect.addAttribute("option",form.getOption());
        }
        if(form.getSearch() != "non"){
            redirect.addAttribute("search",form.getSearch());
        }
        if(form.getSearch_option() != "non"){
            redirect.addAttribute("search_option",form.getSearch_option());
        }
        if(form.getKeyword() != "non"){
            redirect.addAttribute("keyword",form.getKeyword());
        }
        if(form.getMainSearch() != "non"){
            redirect.addAttribute("mainSearch",form.getMainSearch());
        }
        redirect.addAttribute("bno",form.getBoard_bno());
        redirect.addAttribute("kind",form.getBoard_kind());
        redirect.addAttribute("realm",form.getBoard_realm());
        redirect.addAttribute("pageNumber",pageNumber);
        return "redirect:/view";
    }

    @GetMapping("/replyDelete")  //대댓글 삭제
    public String replyDelete(@RequestParam("bno") int bno, @RequestParam("kind") String kind, @RequestParam("realm") String realm, @RequestParam("writer") String writer,@RequestParam("commentBno") int commentBno, Model model, HttpSession session, RedirectAttributes redirect,HttpServletRequest request) throws Exception{
        String pageNumber = null;
        pageNumber = request.getParameter("pageNumber");

        String userID = (String)session.getAttribute("id");
        UserVO user = userMapper.userLogin(userID);
        if(!userID.equals(writer) && user.getUser_manager()==0){
            model.addAttribute("msg","접근할 수 없습니다.");
            model.addAttribute("url","board?kind="+kind+"&realm="+realm);
            return "alert";
        }
        String option = request.getParameter("option");
        if(!option.equals("non")){
            redirect.addAttribute("option",option);
        }
        String search = request.getParameter("search");
        if(!search.equals("non")){
            redirect.addAttribute("search",search);
        }
        String search_option = request.getParameter("search_option");
        if(!search_option.equals("non")){
            redirect.addAttribute("search_option",search_option);
        }
        String keyword = request.getParameter("keyword");
        if(!keyword.equals("non")){
            redirect.addAttribute("keyword",keyword);
        }
        String mainSearch = request.getParameter("mainSearch");
        if(!mainSearch.equals("non")){
            redirect.addAttribute("mainSearch",mainSearch);
        }

        boardMapper.ReplyDelete(commentBno);
        redirect.addAttribute("bno",bno);
        redirect.addAttribute("kind",kind);
        redirect.addAttribute("realm",realm);
        redirect.addAttribute("pageNumber",pageNumber);
        return "redirect:/view";

    }


    @GetMapping("/commentDelete")  //댓글 삭제
    public String commentDelete(@RequestParam("bno") int bno, @RequestParam("kind") String kind, @RequestParam("realm") String realm, @RequestParam("writer") String writer,@RequestParam("commentSequence") int commentSequence, Model model, HttpSession session, RedirectAttributes redirect,HttpServletRequest request) throws Exception{
        String pageNumber = null;
        pageNumber = request.getParameter("pageNumber");
        String userID = (String)session.getAttribute("id");
        UserVO user = userMapper.userLogin(userID);
        if(!userID.equals(writer) && user.getUser_manager()==0){
            model.addAttribute("msg","접근할 수 없습니다.");
            model.addAttribute("url","board?kind="+kind+"&realm="+realm);
            return "alert";
        }
        String option = request.getParameter("option");
        if(!option.equals("non")){
            redirect.addAttribute("option",option);
        }
        String search = request.getParameter("search");
        if(!search.equals("non")){
            redirect.addAttribute("search",search);
        }
        String search_option = request.getParameter("search_option");
        if(!search_option.equals("non")){
            redirect.addAttribute("search_option",search_option);
        }
        String keyword = request.getParameter("keyword");
        if(!keyword.equals("non")){
            redirect.addAttribute("keyword",keyword);
        }
        String mainSearch = request.getParameter("mainSearch");
        if(!mainSearch.equals("non")){
            redirect.addAttribute("mainSearch",mainSearch);
        }
        boardMapper.CommentDelete(commentSequence);
        redirect.addAttribute("bno",bno);
        redirect.addAttribute("kind",kind);
        redirect.addAttribute("realm",realm);
        redirect.addAttribute("pageNumber",pageNumber);
        return "redirect:/view";

    }

    @GetMapping("/cmd_community")
    public String cmdCommunity(HttpSession session, Model model) throws Exception{
        String filename = null;
        if(session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            if (user.getUser_profile() == null) {
                filename = "non";
            } else {
                filename = user.getUser_profile();
            }
        }
        model.addAttribute("filename", filename);
        return "cmd_community";
    }


    @GetMapping("/cmdDetail")
    public String cmdDetail(@RequestParam("realm") String realm,Model model,HttpSession session) throws Exception{
        String filename = null;
        if(session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            if (user.getUser_profile() == null) {
                filename = "non";
            } else {
                filename = user.getUser_profile();
            }
        }
        model.addAttribute("filename", filename);
        model.addAttribute("realm",realm);
        return "cmd_detail";
    }

    @GetMapping("/totalHotTopic")
    public String totalHotTopic(@RequestParam("realm") String realm,@RequestParam("kind") String kind, Model model,HttpSession session) throws Exception{
        String filename = null;
        if(session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            if (user.getUser_profile() == null) {
                filename = "non";
            } else {
                filename = user.getUser_profile();
            }
        }
        List<BoardVO> hotTopic = boardMapper.TotalHotTopicList(realm);
        List<String> hotContentList = new ArrayList<String>();
        for(int i=0; i < hotTopic.size(); i ++){
            String hotContent = hotTopic.get(i).getContent();
            if(hotContent.length() > 55){
                hotContent = hotContent.substring(0,55);
            }
            hotContentList.add(hotContent);
        }
        if(hotTopic.size() >= 1) model.addAttribute("hotTopic1",hotTopic.get(0));
        else model.addAttribute("hotTopic1","non");
        if(hotTopic.size() >= 2) model.addAttribute("hotTopic2",hotTopic.get(1));
        else model.addAttribute("hotTopic2","non");
        if(hotTopic.size() >= 3) model.addAttribute("hotTopic3",hotTopic.get(2));
        else model.addAttribute("hotTopic3","non");
        if(hotTopic.size() >= 4) model.addAttribute("hotTopic4",hotTopic.get(3));
        else model.addAttribute("hotTopic4","non");
        if(hotTopic.size() >= 5) model.addAttribute("hotTopic5",hotTopic.get(4));
        else model.addAttribute("hotTopic5","non");
        if(hotContentList.size() >=1) model.addAttribute("hotContent1", hotContentList.get(0));
        if(hotContentList.size() >=2) model.addAttribute("hotContent2", hotContentList.get(1));
        if(hotContentList.size() >=3) model.addAttribute("hotContent3", hotContentList.get(2));
        if(hotContentList.size() >=4) model.addAttribute("hotContent4", hotContentList.get(3));
        if(hotContentList.size() >=5) model.addAttribute("hotContent5", hotContentList.get(4));
        model.addAttribute("realm",realm);
        model.addAttribute("kind",kind);
        model.addAttribute("filename", filename);
        return "hotTopic";
    }

    @PostMapping("/main_search")
    public String main_search(SearchForm form,Model model,HttpServletRequest request,HttpSession session) throws Exception{
        String keyword = form.getKeyword();
        if(keyword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9].*")) {

            List<BoardVO> board = boardMapper.mainSearch(keyword);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < board.size(); i++) {
                Date reg_date = board.get(i).getReg_date();
                String real_date = format.format(reg_date);
                board.get(i).setReal_date(real_date);
            }
            String pageNumber = "1";
            int off = 0;
            int target2 = 0; //새로
            if (request.getParameter("pageNumber") != null) {
                pageNumber = request.getParameter("pageNumber");
            }
            if (Integer.parseInt(pageNumber) == 1) {
                off = 0;
            } else {
                off = (Integer.parseInt(pageNumber) - 1) * 20;
            }

            int target = (Integer.parseInt(pageNumber) - 1) * 10;
            int target1 = boardMapper.targetPage(target);
            target1 = target1 / 10;
            int startPage = (Integer.parseInt(pageNumber) / 10) * 10 + 1;
            if (Integer.parseInt(pageNumber) % 10 == 0) startPage -= 10;
            int page = Integer.parseInt(pageNumber);
            int count = boardMapper.mainSearchCount(keyword);
            if ((count - (page * 20)) % 10 != 0 && count - (page * 20) > 0) { //새로 매앞에꺼만 20고침
                target2 = ((count - (page * 20)) / 20) + 1; //고침
            } else if ((count - (page * 20)) % 10 == 0 && count - (page * 20) > 0) { //새로
                if ((count - (page * 20)) % 20 > 1) {
                    target2 = ((count - (page * 20)) / 20) + 1;
                } else {
                    target2 = ((count - (page * 20)) / 20); //고침
                }
            } else {
                target2 = 0;
            }
            int resultCount = count - (page * 20); //현재페이지 이후부터의 남은 게시물 갯수
            String filename = null;
            if (session.getAttribute("id") != null) {
                String userID = (String) session.getAttribute("id");
                UserVO user = userMapper.userLogin(userID);
                if (user.getUser_profile() == null) {
                    filename = "non";
                } else {
                    filename = user.getUser_profile();
                }
            }
            boardMapper.searchInsert(keyword);
            model.addAttribute("filename", filename);
            model.addAttribute("board", board); //반환된 결과인 board를 모델로 담아 search.html로 전달
            model.addAttribute("keyword", keyword); //키워드 전달
            model.addAttribute("startPage", startPage);
            model.addAttribute("page", page);
            model.addAttribute("count", resultCount);
            model.addAttribute("target2", target2);
            return "main_search";
        } else {
            model.addAttribute("msg","검색어를 입력해주세요.");
            model.addAttribute("url","back"); //메세지와 url을 모델로 담아 알림창을 띄울 alert.html로 전달
            return "alert";
        }
    }

    @GetMapping("/main_search")
    public String main_search2(SearchForm form,Model model,HttpServletRequest request, @RequestParam("keyword") String keyword,HttpSession session) throws Exception{
        if(keyword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9].*")) {
            List<BoardVO> board = boardMapper.mainSearch(keyword);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < board.size(); i++) {
                Date reg_date = board.get(i).getReg_date();
                String real_date = format.format(reg_date);
                board.get(i).setReal_date(real_date);
            }
            String pageNumber = "1";
            int off = 0;
            int target2 = 0; //새로
            if (request.getParameter("pageNumber") != null) {
                pageNumber = request.getParameter("pageNumber");
            }
            if (Integer.parseInt(pageNumber) == 1) {
                off = 0;
            } else {
                off = (Integer.parseInt(pageNumber) - 1) * 20;
            }

            int target = (Integer.parseInt(pageNumber) - 1) * 10;
            int target1 = boardMapper.targetPage(target);
            target1 = target1 / 10;
            int startPage = (Integer.parseInt(pageNumber) / 10) * 10 + 1;
            if (Integer.parseInt(pageNumber) % 10 == 0) startPage -= 10;
            int page = Integer.parseInt(pageNumber);
            int count = boardMapper.mainSearchCount(keyword);
            if ((count - (page * 20)) % 10 != 0 && count - (page * 20) > 0) { //새로 매앞에꺼만 20고침
                target2 = ((count - (page * 20)) / 20) + 1; //고침
            } else if ((count - (page * 20)) % 10 == 0 && count - (page * 20) > 0) { //새로
                if ((count - (page * 20)) % 20 > 1) {
                    target2 = ((count - (page * 20)) / 20) + 1;
                } else {
                    target2 = ((count - (page * 20)) / 20); //고침
                }
            } else {
                target2 = 0;
            }
            int resultCount = count - (page * 20); //현재페이지 이후부터의 남은 게시물 갯수
            String filename = null;
            if (session.getAttribute("id") != null) {
                String userID = (String) session.getAttribute("id");
                UserVO user = userMapper.userLogin(userID);
                if (user.getUser_profile() == null) {
                    filename = "non";
                } else {
                    filename = user.getUser_profile();
                }
            }
            model.addAttribute("filename", filename);
            model.addAttribute("board", board); //반환된 결과인 board를 모델로 담아 search.html로 전달
            model.addAttribute("keyword", keyword); //키워드 전달
            model.addAttribute("startPage", startPage);
            model.addAttribute("page", page);
            model.addAttribute("count", resultCount);
            model.addAttribute("target2", target2);
            return "main_search";
        } else {
            model.addAttribute("msg","검색어를 입력해주세요.");
            model.addAttribute("url","back"); //메세지와 url을 모델로 담아 알림창을 띄울 alert.html로 전달
            return "alert";
        }
    }

    @GetMapping("/cmd_roadmap")
    public String cmdRoadmap(HttpSession session, Model model) throws Exception{
        String userID;
        String filename;
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
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
        return "cmd_roadmap";
    }




    @ResponseBody
    @RequestMapping(value = "/searchRanking", method = RequestMethod.POST)
    public void getSearchRanking(HttpServletRequest req, HttpSession session, HttpServletResponse response) throws Exception{
        req.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        List<String> ranking = boardMapper.searchRanking();

        if(ranking.size() == 0){
            try{
                response.getWriter().print("");
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        else {

            StringBuffer result = new StringBuffer("");
            result.append("{\"result\":[");
            for (int i = 0; i < ranking.size(); i++) {
                result.append("[{\"value\": \"" + ranking.get(i) + "\"}]");
                if (i != ranking.size() - 1) result.append(",");
            }
            result.append("]}");
            try {
                response.getWriter().print(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


}
