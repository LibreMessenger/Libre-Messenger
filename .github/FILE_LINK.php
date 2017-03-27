<?php
$json = file_get_contents('https://api.github.com/repos/kriztan/Pix-Art-Messenger/releases/latest');
$data = json_decode($json);
$filename = $data->assets[0]->name;
$filesize = $data->assets[0]->size;
header('Content-Type: application/octet-stream');
header('Content-Disposition: attachment; filename="'.$filename.'";');
header('Content-Length: '.$filesize);
readfile($data->assets[0]->browser_download_url);
?>
