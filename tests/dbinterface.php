<?php
// There to test db access
 include 'xmlrpc.inc';
 include 'xmlrpcs.inc';

 function insertValues ($params) {

     $xval = $params->getParam(0);
     for ($i=0;$i < $xval->arraysize();$i++) {
      $temp=$xval->arraymem($i);
      $c[$i]=$temp->scalarval(); 
      $d="$c[$i] "."$d";
     }
     $db = mysql_connect("localhost", "vanderaa", "pmx;rgJ");
     mysql_select_db("ruches",$db);
     $result=mysql_query("insert into tinidata values (null,curdate(),curtime(),$c[0],$c[1],$c[2],$c[3],$c[4],$c[5],$c[6],$c[7])");
     
//     $fichier=fopen("test.txt","a");
//     fwrite($fichier,"valeur: $d \n");
//     fclose($fichier);
     return new xmlrpcresp(new xmlrpcval($d, 'string'));
  }

new xmlrpc_server(array('insertValues' =>
                  array('function' => 'insertValues',
                        'signature' => $insertValues_sig,
                        'docstring' => $insertValues_doc)));



?>
