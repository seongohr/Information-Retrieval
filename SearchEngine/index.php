<?php
  include 'SpellCorrector.php';
  include 'simple_html_dom.php';
  // make sure browsers see this page as utf-8 encoded HTML
  header('Content-Type: text/html; charset=utf-8');
  header("Access-Control-Allow-Origin: *");

  define("limit", 10);
  $query = isset($_REQUEST['search']) ? $_REQUEST['search'] : false; 
  $page_rank_algo = isset($_REQUEST['pageRank']) ? $_REQUEST['pageRank'] : false;
  $additionalParameters = false;
  $results = false;
  $correction = 0;

  function spellCheck($q) {
    $words = preg_split("/[\s,]+/", $q);
    $result = "";
    foreach ($words as $value) {
      $correct = SpellCorrector::correct($value);
      // echo ('correct'.$correct);
      if ($correct != $value) {
        $correction = 1;
      }
      $result = $result.$correct." ";
    }
    $result = trim($result, " ");
    return array('result'=>$result, 'correction'=> $correction);
  }

  $lines = explode("\n", file_get_contents('URLtoHTML_nytimes_news.csv'));
  $headers = str_getcsv(array_shift($lines));
  $idToUrl = array();

  foreach ($lines as $line) {
    $row = explode(',', $line);
    $idToUrl[$row[0]] = $row[1];
  }

  if ($query && $page_rank_algo) {
    require_once('./Apache/Solr/Service.php');
    // create a new solr service instance - host, port, and corename 
    // path (all defaults in this example)
    $solr = new Apache_Solr_Service('localhost', 8983, '/solr/myexample/');
    // if magic quotes is enabled then stripslashes will be needed
    if (get_magic_quotes_gpc() == 1) {
      $query = stripslashes($query); 
    }
    if ($page_rank_algo == 'external') {
      $additionalParameters = array(
        'sort'=>'pageRankFile desc',
        'fl'=>array('id', 'og_description', 'title', 'og_url'),
      );
    }
    else {
      $additionalParameters = array(
        'fl'=>array('id', 'og_description', 'title', 'og_url'),
      );
    }

    $temp = spellCheck($query);
    $result = $temp['result'];
    $correction = $temp['correction'];

    try
    { 
      $results = $solr->search($query, 0, limit, $additionalParameters);
      // $results = $solr->search($result, 0, $limit, $additionalParameters);
    }
    catch (Exception $e) {
      die("<html><head><title>SEARCH EXCEPTION</title><body><pre>{$e->__toString()}</pre></body></html>");
    }
  }
  ?> 
  <html>
    <head>
      <title>Search Engine</title> 
      <link rel="stylesheet" href="//code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css">
      <script src="https://code.jquery.com/jquery-1.12.4.js"></script>
      <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>
      <script>
      //autocomplete
      $(document).ready(function() {
        console.log(<?=$correction?>)
        $("#search").autocomplete({
          source: function( request, response) {
            var search_term = $.trim($("#search").val().toLowerCase());
            var start_term = "";
            var index = search_term.lastIndexOf(" ");
            if(index != -1) {
              start_term = search_term.substring( 0, index );
              search_term = search_term.substring( index + 1 );
            }
            var url = 'http://localhost:8983/solr/myexample/suggest?indent=on&q=' + search_term + '&wt=json';
            $.ajax({
              url: 'autocomplete.php',
              type: 'GET',
              dataType: 'json',
              data: {
                address: url
              },
              success: function(data) {
                var output=[];
                data.suggest.suggest[search_term].suggestions.forEach(function(x){
                  output.push(start_term + " " + x.term);
                });
                response(output);
              }
            });
          },
          minLength: 1,
        });

        $("#correct").click(function() {
          query = $(this).text();
          $("#search").attr("value", query);
          $("#myForm").submit();
        });

      });
      </script>
      <style>
        table {
          border: 1px solid black;
          /* border-bottom: 1px solid black;  */
          text-align: left; 
          width: 1200px; 
          margin: 20 10 20 10
        }
        th {
          padding: 5px 10px 5px;  
        }
        span {
          color: blue;
          /* font-style: italic; */
          /* font-weight: bold; */

        }
        .result_c, .result_q {
          font-size : 20px;
        }
        .correct:hover {
          text-decoration: underline;
        }
      </style>
    </head>
    <body>
      <form id="myForm" accept-charset="utf-8" method="get" autocomplete="off">
        <label for="search">Search:</label>
          <input id="search" name="search" type="text" onkeyup="autocomplete(this.value)" value="<?php echo htmlspecialchars($query, ENT_QUOTES, 'utf-8'); ?>"/>
          <input 
            id="lucene" 
            name="pageRank" 
            type="radio" 
            value="lucene"
            <?php if(isset($_REQUEST['pageRank']) && $_REQUEST['pageRank'] == 'lucene')  echo ' checked="checked"';?>
          />
          <label for="lucene">Lucene(Default)</label>
          <input 
            id="external" 
            name="pageRank" 
            type="radio" 
            value="external"
            <?php if(isset($_REQUEST['pageRank']) && $_REQUEST['pageRank'] == 'external')  echo ' checked="checked"';?>
          />
          <label for="external">External Page Rank</label>
          <input type="submit"/>
      </form> 
  <?php
  // display results
  if ($results) {
    $total = (int) $results->response->numFound; 
    $start = min(1, $total);
    $end = min(limit, $total);
    if($correction == 1) {
  ?>  
      <div class="result_c">Did you mean: <span id="correct" class="correct"><?=$result;?></span></div>
  <?php
    }
  ?>  <div class="result_q">Results for <span><?=$query;?></span></div><br>
      <div>Results <?php echo $start; ?> - <?php echo $end;?> of <?php echo $total; ?>:</div>
      <ol> 
  <?php
    // iterate result documents
    foreach ($results->response->docs as $doc)
    { 
      ?>
        <li>
          <table>
      <?php
      // iterate document fields / values
      $title = '';
      $url = '';
      $id = '';
      $description = 'N/A';
      foreach ($doc as $field => $value)
      { 
        if ($field == 'id') {
          $id = $value;
        } 
        else if ($field == 'title') {
          $title = $value;
        }
        else if ($field == 'og_description') {
          $description = $value;
        }
        else if ($field == 'og_url') {
          $url = $value;
        }
      }
      if ($url == '') {
        $key = str_replace("/Users/seongohryoo/Desktop/USC/spring2020/csci572/assignments/assignments4/solr-7.7.2/nytimes/"
                            , '', $id);
        $url = $idToUrl[$key];
      }
      ?>
            <tr>
              <th>Title</th> 
              <td>
                <a href=<?php echo $url; ?>>
                  <?php echo htmlspecialchars($title, ENT_NOQUOTES, 'utf-8'); ?>
                </a>
              </td> 
            </tr>
            <tr>
              <th>URL</th>
              <td>
                <a href=<?php echo $url; ?>>
                    <?php echo htmlspecialchars($url, ENT_NOQUOTES, 'utf-8'); ?>
                </a>
              </td> 
            </tr>
            <tr>
              <th>ID</th> 
              <td><?php echo htmlspecialchars($id, ENT_NOQUOTES, 'utf-8'); ?></td> 
            </tr>
            <tr>
              <th>Description</th> 
              <td><?php echo htmlspecialchars($description, ENT_NOQUOTES, 'utf-8'); ?></td> 
            </tr> 
          </table>
          </li>
<?php 
    }
?> 
        </ol>   
<?php 
  }
?>
  </body> 
</html>