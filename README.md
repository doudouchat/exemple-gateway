[![Codacy Badge](https://api.codacy.com/project/badge/Grade/96f5ace9ca79448db55b201ac7a9781b)](https://app.codacy.com/gh/doudouchat/exemple-gateway?utm_source=github.com&utm_medium=referral&utm_content=doudouchat/exemple-gateway&utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.com/doudouchat/exemple-gateway.svg?branch=master)](https://travis-ci.com/doudouchat/exemple-gateway)
[![codecov](https://codecov.io/gh/doudouchat/exemple-gateway/graph/badge.svg)](https://codecov.io/gh/doudouchat/exemple-gateway) 

# exemple-gateway

## maven

<p>execute <code>mvn clean install</code></p>

## Docker

<ol>
<li>docker build -t exemple-gateway .</li>
</ol>

<ol>
<li>docker-compose up -d gateway</li>
<li>browser: docker-compose -f docker-compose.yml -f docker-compose.browser.yml up -d gateway</li>
</ol>
