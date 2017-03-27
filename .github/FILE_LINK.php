<?php
$json = file_get_contents('https://api.github.com/repos/kriztan/Pix-Art-Messenger/releases/latest');
$data = json_decode($json);
echo $data->assets[0]->browser_download_url;
?>
