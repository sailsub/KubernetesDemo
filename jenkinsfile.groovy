node {
    stage ('Git checkout') {
       git branch: 'main', credentialsId: 'Gitlab', url: 'https://github.com/sailsub/KubernetesDemo.git' 
    }
    stage ('SSH dockerfiles to ansible server') {
        sshagent(['AnsibleServer']) {
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202'
           sh 'scp /var/lib/jenkins/workspace/kubernetesdemo/* ubuntu@3.109.211.202:/home/ubuntu'
        }
    }
    stage ('Building docker images') {
         sshagent(['AnsibleServer']) {
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 cd /home/ubuntu'
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 docker image build -t $JOB_NAME:v1.$BUILD_ID .'
        }
    }
    stage ('Tagging docker images') {
         sshagent(['AnsibleServer']) {
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 cd /home/ubuntu'
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 docker image tag $JOB_NAME:v1.$BUILD_ID sailsub/$JOB_NAME:v1.$BUILD_ID'
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 docker image tag $JOB_NAME:v1.$BUILD_ID sailsub/$JOB_NAME:latest'
        }
    }
    stage ('Push docker image to docker hub') {
        sshagent(['AnsibleServer']) {
            withCredentials([string(credentialsId: 'dockerhub_passwd', variable: 'dockerhub_passwd')]) {
                sh "ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 docker login -u sailsub@gmail.com -p ${dockerhub_passwd}"
                sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 docker image push sailsub/$JOB_NAME:v1.$BUILD_ID'
                sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 docker image push sailsub/$JOB_NAME:latest'
            }
             
        }
    }
    stage ('Copy files from ansible to Kubernetes server'){
        sshagent(['KubernetesServer']) {
            sh 'ssh -o StrictHostKeyChecking=no ubuntu@13.233.117.175'
            sh 'scp /var/lib/jenkins/workspace/kubernetesdemo/* ubuntu@13.233.117.175:/home/ubuntu'
        }
    }
    stage ('Kubernetes deplpoyment using ansible'){
        sshagent(['AnsibleServer']) {
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 cd /home/ubuntu'
           sh 'ssh -o StrictHostKeyChecking=no ubuntu@3.109.211.202 ansible-playbook ansible.yml'
        }
    }

}