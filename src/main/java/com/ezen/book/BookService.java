package com.ezen.book;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ezen.utill.PageVO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

public class BookService {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private final String path = "/WEB-INF/views/book/";
    private final String uploadPath = "d:/upload/img";

    public BookService(HttpServletRequest request, HttpServletResponse response) {
	this.request = request;
	this.response = response;
    }

    public String exec() throws IOException, ServletException {

	String cmd = request.getParameter("cmd");
	String view = null;

	if (cmd == null || cmd.equals("list")) {
	    return BookListService();
	} else if (cmd.equals("view")) {
	    return BookViewService();
	} else if (cmd.equals("new")) {
	    return BookNewService();
	} else if (cmd.equals("edit")) {
	    return BookUpdateService();
	} else if (cmd.equals("imgDown")) {
	    return BookImgDownService();
	} else if (cmd.equals("del")) {
	    return BookDeleteService();
	} else if (cmd.equals("ssave")) {
	    BookScoreSaveService();
	} else if (cmd.equals("slist")) {
	    BookScoreListService();
	}

	return null;
    }

    private void BookScoreListService() throws IOException {
	
	// 파라메타 값 받기
	int bno = 39;
	String strbno = request.getParameter("bno").trim();
	if (strbno != null && strbno != "")
	    bno = Integer.parseInt(strbno);
	int page = 1;
	String strpage = request.getParameter("page").trim();
	if (strpage != null && strpage != "")
	    page = Integer.parseInt(strpage);

	// 페이지
	BookscoreDAO dao = BookscoreDAO.getInstance();

	// 특정 도서(bno값) 전체 개수 세기
	int rowCnt = dao.getRowCount(bno);

	// 다음페이지가 있을지??
	// int page, int totalCount, int displayRow, int displayPage
	int displayRow = 5;
	PageVO pvo = new PageVO(page, rowCnt, displayRow, 0);
	boolean next = pvo.nextPageScore();
	System.out.println("next=" + next);

	// 해당페이지 검색하기
	List<BookscoreVO> list = dao.getBookScore(page, bno, displayRow);

	// json 데이터를 조립해서 호출쪽으로 출력해 준다.
	// json데이터 조립 :gson 라이브러리 사용
	if (list != null) {
	    JsonObject jObj = new JsonObject();
	    jObj.addProperty("next", next);// 더보기버튼 활성화

	    // list를 json array로 만들기
	    JsonArray arr = new JsonArray();

	    // json 객체 만들기
	    JsonObject data = null;
	    for (BookscoreVO vo : list) {
		data = new JsonObject();
		data.addProperty("score", vo.getScore());
		data.addProperty("id", vo.getId());
		data.addProperty("cmt", vo.getCmt());
		arr.add(data);
	    }

	    jObj.add("arr", arr);

	    // Gson gson=new Gson();
	    // System.out.println(gson.toJson(jObj));
	    // 보내기 전에 encoding
	    response.setContentType("application/json;charset=utf-8");
	    PrintWriter out = response.getWriter();
	    out.print(jObj);
	    out.flush();
	    out.close();
	}

    }

    private void BookScoreSaveService() throws IOException {

	// 파라메타 값 받기
	String id = request.getParameter("id");
	if (id == null || id == "")
	    id = "hong1";
	String strbno = request.getParameter("bno");
	int bno = 39;
	if (strbno != null && strbno != "")
	    bno = Integer.parseInt(strbno);
	String strscore = request.getParameter("score").trim();
	double score = 0;
	if (strscore != null)
	    score = Double.parseDouble(strscore);
	String cmt = request.getParameter("cmt");

	// dao 객체 생성
	BookscoreDAO dao = BookscoreDAO.getInstance();

	// vo 객체 생성
	BookscoreVO vo = new BookscoreVO(0, bno, id, score, cmt, null);

	// dao 삽입 메소드 수행
	dao.insertBookscore(vo);

	// 페이지 이동이 필요 없다
	// 비동기 통신 메소드를 다 수행하고나면 불렀던 곳으로 다시 돌아가서 callback 호출

	// 반환 해주는 데이터가 있다.
	// BookscoreDAO 객체 필요
	BookscoreDAO sdao = BookscoreDAO.getInstance();
	ArrayList<Number> scoreS = sdao.getAvgscore(bno);

	request.setAttribute("avgScore", scoreS.get(0));
	request.setAttribute("cnt", scoreS.get(1));

	// json data 보낸다.
	JsonObject ob = new JsonObject();
	ob.addProperty("avgScore", scoreS.get(0));
	ob.addProperty("cnt", scoreS.get(1));

	// 보내기 전에 encoding
	response.setContentType("application/json;charset=utf-8");
	PrintWriter out = response.getWriter();
	out.print(ob);
	out.flush();
	out.close();
    }

    private String BookDeleteService() throws ServletException, IOException {
	// 파라메타 받기
	int bno = Integer.parseInt(request.getParameter("bno"));

	// dao 객체생성
	BookDAO dao = BookDAO.getInstance();

	// dao 메소드 구현
	int result = dao.deleteBook(bno);
	if (result == 1) {// 삭제 완료
	    RequestDispatcher rd = request.getRequestDispatcher("bList");
	    rd.forward(request, response);
	}

	return null;
    }

    private String BookImgDownService() throws IOException {

	// 파라메타값 받아오기
	String originFname = request.getParameter("originFname");// 원본파일명
	String upload = request.getParameter("upload");// 경로
	String saveFname = request.getParameter("saveFname");// 저장된 파일명
	String filename = upload + "/" + saveFname;

	// 웹브라우저의 종류 확인
	String agent = request.getHeader("User-Agent");

	// ie 7 또는 edge
	boolean ieBrowser = (agent.indexOf("Trident") > -1) || (agent.indexOf("Edge") > -1);
	if (ieBrowser) {
	    originFname = URLEncoder.encode(originFname, "utf-8").replace("\\", "%20");
	} else {// edge, 파이어폭스, 크롬
	    originFname = new String(originFname.getBytes("utf-8"), "iso-8859-1");
	}
	response.setContentType("image/jpg");

	// 다운로드 되는 파일명 설정
	response.setHeader("Content-Disposition", "attachment;filename=" + originFname);
	FileInputStream in = new FileInputStream(filename);// 파일 open

	// 출력할 곳
	BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
	int numRead;
	byte b[] = new byte[4096];// 4K만큼
	while ((numRead = in.read(b, 0, b.length)) != -1) {
	    out.write(b, 0, numRead);
	} // end while
	out.flush();// 버퍼에 남은것 출력
	in.close();
	out.close();
	return null;
    }

    private String BookUpdateService() throws IOException {

	// mr 객체만 생성하면 mr안에 모든 넘어온 파라메타가 다 저정되고, 업로드 파일이 있으면 파일 저장폴더에 저장 된다
	MultipartRequest mr = new MultipartRequest(request, uploadPath, 2 * 1024 * 1024, "utf-8",
		new DefaultFileRenamePolicy());

	// 넘어오는 데이터를 받아서 수정작업
	// MultipartRequest 사용
	// 첨부파일 있는지 없는지 확인?? 있다면 수정, 없다면 수정 x
	if (mr != null) {

	    // BookVO 넘어온 데이터를 담고 수정작업
	    // 넘어온 데이터 받기
	    // bno 받기
	    int bno = Integer.parseInt(mr.getParameter("bno"));
	    String title = mr.getParameter("title").trim();
	    String writer = mr.getParameter("writer").trim();
	    String publisher = mr.getParameter("publisher").trim();
	    String content = mr.getParameter("content").trim();
	    int price = Integer.parseInt(mr.getParameter("price"));
	    String oFileName = mr.getOriginalFileName("file");
	    // if(oFileName==null)uploadPath=null;
	    BookVO vo = new BookVO(bno, title, writer, price, publisher, content, oFileName,
		    mr.getFilesystemName("file"), uploadPath);

	    // dao 객체 만들기
	    BookDAO dao = BookDAO.getInstance();

	    // dao 수정 메소드 호출
	    int result = dao.updateBook(vo);
	    if (result == 1) {// 업데이트 완료

		// 상세화면 bookView.jsp 여기로 보내주는 서블릿 /bView?bno
		return "book?cmd=view&bno=" + bno;
	    }
	}
	return null;
    }

    private String BookNewService() throws IOException {

	String method = request.getMethod().toUpperCase();

	if (method.equals("GET")) {
	    return path + "bookNew.jsp";
	} else {// post

	    // 업로드 폴더 설정
	    File fpath = new File(uploadPath);
	    if (!fpath.exists()) {// 폴더가 없으면 폴더 만들기
		fpath.mkdirs();
	    }

	    // MultipartRequest 객체 생성하면 파일업로드 완료
	    MultipartRequest mr = new MultipartRequest(request, uploadPath, 2 * 1024 * 1024, "utf-8",
		    new DefaultFileRenamePolicy());
	    if (mr != null) {

		// 넘어온 데이터 받기
		String title = mr.getParameter("title").trim();
		String writer = mr.getParameter("writer").trim();
		String publisher = mr.getParameter("publisher").trim();
		String content = mr.getParameter("content").trim();
		int price = Integer.parseInt(mr.getParameter("price"));

		// VO 객체에 넘어온 데이터 저장
		BookVO vo = new BookVO(title, writer, price, publisher, content, mr.getOriginalFileName("file"),
			mr.getFilesystemName("file"), uploadPath);

		// dao 객체 생성
		BookDAO dao = BookDAO.getInstance();

		// dao 객체 insertBook 메소드 수행
		int result = dao.insertBook(vo);

		// 페이지이동 리스트
		if (result == 1) {// 삽입완료 - 전체목록 이동
		    return "book?cmd=list";

		}
	    }

	}

	return null;
    }

    private String BookViewService() {
	// 파라메타 받기 bno
	int bno = Integer.parseInt(request.getParameter("bno"));

	// 페이지 관련 정보 받아서
	String strpage = request.getParameter("page");
	int page = 1;
	if (strpage != null)
	    page = Integer.parseInt(strpage);
	String searchword = request.getParameter("searchword");
	String searchtype = request.getParameter("searchtype");

	// dao 객체 생성
	BookDAO dao = BookDAO.getInstance();

	// dao 메소드 호출 결과 받기 : BookVO (모든 필드 다 담겨있어야 한다)
	BookVO vo = dao.getBook(bno);

	// 페이지 이동전 jsp 필요한데이터를 setAttribute로 담는다
	if (vo != null) {// 조회 완료
	    // 현도서의 평균평점을 조회해서 브라우저에서 접근가능한 객체에 값을 저장
	    // BookscoreDAO 객체 필요
	    BookscoreDAO sdao = BookscoreDAO.getInstance();
	    ArrayList<Number> score = sdao.getAvgscore(bno);
	    request.setAttribute("avgScore", score.get(0));
	    request.setAttribute("cnt", score.get(1));
	    request.setAttribute("vo", vo);

	    // page관련정보 담는다
	    request.setAttribute("page", page);
	    request.setAttribute("searchword", searchword);
	    request.setAttribute("searchtype", searchtype);
	    return path + "bookView.jsp";
	}
	return null;
    }

    private String BookListService() {

	BookDAO dao = BookDAO.getInstance();
	// List<BookVO> list=dao.getBookList();// 전체 행을 출력

	// 페이징을 위한 변수 선언
	// parmaPage : 파라메타(?뒤에 ?page=${i})---현재 페이지 설정
	String parmaPage = request.getParameter("page");

	int page = 0;
	if (parmaPage == null)
	    page = 1;
	else
	    page = Integer.parseInt(parmaPage);

	int displayRow = 5;
	int displayPage = 5;
	int rowCnt = 0;
	List<BookVO> list = null;

	// 검색어 관련 파라메타 받아서 저장
	String searchtype = request.getParameter("searchtype");
	String searchword = request.getParameter("searchword");

	if (searchword == null || searchword.equals("")) {
	    list = dao.getBookList(page, displayRow);
	    rowCnt = dao.getRowConut();// booktbl 전체 행의 개수
	} else {// 검색어 있음
		// searchtype=
	    list = dao.getBookList(page, displayRow, searchtype, searchword);
	    rowCnt = dao.getRowConut(searchtype, searchword);// booktbl 해당검색어가 있는 전체 행의 개수
	}

	// PageVO 객체 생성
	PageVO pVo = new PageVO(page, rowCnt, displayRow, displayPage);
	pVo.setSearchword(searchword);// 검색어를 PageVO 객체에 저장
	pVo.setSearchtype(searchtype);
	// pVo.setPage(page);//page설정
	// pVo.setPage(6);
	// pVo.setTotalCount(rowCnt);
	// 브라우저에서 접근가능한 객체에 list 저장
	request.setAttribute("list", list);
	request.setAttribute("pVo", pVo);

	return path + "booklist.jsp";
    }

}
