# 📋 Infisical Migration Plan

Step-by-step migration plan from .env files to Infisical.

## Timeline: 2 weeks

### Week 1: Preparation and Local Development

#### Day 1-2: Setup and Preparation

- [ ] **Team training** (2 hours)
  - Infisical presentation and benefits overview
  - CLI usage demonstration
  - New workflow overview
  
- [ ] **Infisical account setup**
  - Create organization "Consultant"
  - Create project "consultant-backend"
  - Configure environments (dev, staging, prod)
  - Add team members with proper roles
  - Enable MFA for everyone

- [ ] **CLI installation for the entire team**

  ```bash
  brew install infisical/get-cli/infisical  # macOS
  # Linux - see INFISICAL_QUICKSTART.md
  ```

#### Day 3-4: Development environment

- [ ] **Migrate development secrets**

  ```bash
  cd /home/lvn/prg/scala/Consultant/backend
  infisical init
  ./scripts/setup-infisical-secrets.sh
  ```

- [ ] **Local development testing**
  - Each developer tests `infisical run --env=development -- sbt compile`
  - Verify IntelliJ IDEA / VS Code integration
  - Document issues

- [ ] **Update README.md**
  - Add Infisical instructions to onboarding
  - Update quick start guide
  - Add troubleshooting tips

#### Day 5: Staging preparation

- [ ] **Create staging secrets**

  ```bash
  # Generate strong secrets for staging
  infisical secrets set JWT_SECRET "$(openssl rand -base64 64)" --env=staging
  infisical secrets set DB_ENCRYPTION_KEY "$(openssl rand -base64 32)" --env=staging
  infisical secrets set SESSION_SECRET "$(openssl rand -base64 32)" --env=staging
  ```

- [ ] **Docker Compose testing**

  ```bash
  # Get service token for staging
  infisical service-token --env=staging
  
  # Create .env.infisical
  cat > .env.infisical << EOF
  INFISICAL_TOKEN=st.staging.xxx
  INFISICAL_PROJECT_ID=xxx
  INFISICAL_ENVIRONMENT=staging
  EOF
  
  # Test
  docker-compose -f docker-compose.infisical.yml --env-file .env.infisical up
  ```

### Week 2: Staging and Production deployment

#### Day 6-7: Staging deployment

- [ ] **Kubernetes Operator setup**

  ```bash
  # Add Helm repo
  helm repo add infisical https://infisical.com/helm-charts
  helm repo update
  
  # Install operator in staging cluster
  helm install infisical-operator infisical/infisical-secrets-operator \
    --namespace infisical-operator-system \
    --create-namespace
  ```

- [ ] **Deploy to staging K8s**

  ```bash
  # Create service token for K8s
  infisical service-token --env=staging --scope=read
  
  # Create auth secret
  kubectl create secret generic infisical-auth \
    --from-literal=token='st.staging.xxx' \
    --namespace consultant-backend-staging
  
  # Apply InfisicalSecret CR
  kubectl apply -f kubernetes/infisical-secret.yaml -n consultant-backend-staging
  
  # Deploy app
  kubectl apply -f kubernetes/deployment.yaml -n consultant-backend-staging
  ```

- [ ] **Staging testing** (Full QA cycle)
  - Health checks
  - Authentication flow
  - Database connectivity
  - Redis caching
  - Load testing
  - Security audit

#### Day 8: Production secrets preparation

- [ ] **Generate production secrets**

  ```bash
  # Strong random secrets
  infisical secrets set JWT_SECRET "$(openssl rand -base64 64)" --env=production
  infisical secrets set DB_ENCRYPTION_KEY "$(openssl rand -base64 32)" --env=production
  infisical secrets set SESSION_SECRET "$(openssl rand -base64 32)" --env=production
  infisical secrets set DB_PASSWORD "$(openssl rand -base64 32)" --env=production
  infisical secrets set REDIS_PASSWORD "$(openssl rand -base64 24)" --env=production
  ```

- [ ] **Configure production database credentials**
  - Update DB_HOST, DB_PORT, DB_NAME
  - Setup dynamic secrets (optional)
  
- [ ] **Configure production Redis**
  - Update REDIS_HOST, REDIS_PORT

- [ ] **Setup audit webhooks**
  - CloudWatch/Slack notifications
  - Critical secret change alerts

#### Day 9: Production deployment preparation

- [ ] **Backup existing production secrets**

  ```bash
  # Backup current .env files (encrypted)
  tar -czf prod-secrets-backup-$(date +%Y%m%d).tar.gz .env.production
  openssl enc -aes-256-cbc -salt -in prod-secrets-backup-*.tar.gz \
    -out prod-secrets-backup-*.tar.gz.enc
  
  # Upload to secure S3 bucket
  aws s3 cp prod-secrets-backup-*.tar.gz.enc \
    s3://your-secrets-backup-bucket/
  ```

- [ ] **Production Kubernetes preparation**

  ```bash
  # Install operator in prod cluster
  helm install infisical-operator infisical/infisical-secrets-operator \
    --namespace infisical-operator-system \
    --create-namespace \
    --set image.tag=v1.2.0  # pinned version
  
  # Create prod service token (read-only)
  infisical service-token --env=production --scope=read --ttl=90d
  
  # Create auth secret
  kubectl create secret generic infisical-auth \
    --from-literal=token='st.prod.xxx' \
    --namespace consultant-backend
  ```

- [ ] **CI/CD pipeline update**

  ```yaml
  # GitHub Actions / GitLab CI
  - name: Deploy with Infisical
    env:
      INFISICAL_TOKEN: ${{ secrets.INFISICAL_PRODUCTION_TOKEN }}
    run: |
      infisical run --env=production -- kubectl apply -f k8s/
  ```

#### Day 10: Production deployment (Blue-Green)

##### Maintenance window: Plan for low-traffic period

- [ ] **T-1 hour: Pre-deployment checks**

  ```bash
  # Verify Infisical availability
  curl https://app.infisical.com/api/status
  
  # Verify all secrets exist
  infisical secrets list --env=production
  
  # Test InfisicalSecret CR in staging
  kubectl get infisicalsecret -n consultant-backend-staging
  ```

- [ ] **T-0: Blue-Green deployment start**

  ```bash
  # Deploy Green (new version with Infisical)
  kubectl apply -f kubernetes/infisical-secret.yaml -n consultant-backend
  kubectl apply -f kubernetes/deployment.yaml -n consultant-backend
  
  # Wait for healthy pods
  kubectl rollout status deployment/consultant-backend -n consultant-backend
  
  # Verify secrets loaded
  kubectl exec -it deployment/consultant-backend -n consultant-backend -- \
    cat /run/secrets/JWT_SECRET | wc -c  # should be 64+ chars
  ```

- [ ] **T+10min: Smoke tests**
  - Health endpoint check
  - Login test
  - Database query test
  - Redis cache test
  - JWT token generation/validation

- [ ] **T+30min: Traffic migration**

  ```bash
  # Switch load balancer to Green
  kubectl patch service consultant-backend -n consultant-backend \
    -p '{"spec":{"selector":{"version":"green"}}}'
  
  # Monitor metrics
  kubectl top pods -n consultant-backend
  ```

- [ ] **T+1hour: Monitoring**
  - CloudWatch metrics
  - Error rate
  - Response time
  - Authentication success rate
  - Infisical audit logs

- [ ] **T+2hours: Blue cleanup**

  ```bash
  # Scale down old Blue deployment
  kubectl scale deployment consultant-backend-blue --replicas=0
  
  # Delete old secrets (after verification)
  # DO NOT delete yet - keep for rollback
  ```

#### Day 11-12: Post-deployment

- [ ] **Monitoring and alerts**
  - Check Infisical audit logs daily
  - Review CloudWatch alarms
  - Monitor failed secret access attempts
  
- [ ] **Team feedback**
  - Survey developers on new workflow
  - Document lessons learned
  - Update runbooks

- [ ] **Cleanup old secrets**

  ```bash
  # After 3 days of stable production
  # Remove old .env files from servers
  # Delete old Kubernetes secrets
  # Update documentation
  ```

- [ ] **Setup rotation schedule**
  - JWT_SECRET: 90 days
  - DB_PASSWORD: 90 days (or dynamic secrets)
  - SESSION_SECRET: 90 days
  - API keys: per provider policy

#### Day 13-14: Optimization

- [ ] **Enable dynamic secrets for PostgreSQL**

  ```bash
  # In Infisical UI: Settings → Dynamic Secrets → PostgreSQL
  # Configure auto-rotation every 1 hour
  ```

- [ ] **Setup secret versioning policies**
  - Retention: 30 versions
  - Auto-cleanup old versions

- [ ] **Configure advanced RBAC**

  ```yaml
  Developers:
    - read: development, staging
    - write: development
    - no access: production
  
  DevOps/SRE:
    - read/write: all environments
    - manage: service tokens, RBAC
  
  CI/CD:
    - read only: production
    - specific secrets: deployment credentials
  ```

## Rollback Plan

If something goes wrong:

### Immediate rollback (< 5 minutes)

```bash
# 1. Switch back to Blue deployment
kubectl patch service consultant-backend -n consultant-backend \
  -p '{"spec":{"selector":{"version":"blue"}}}'

# 2. Scale up Blue, scale down Green
kubectl scale deployment consultant-backend-blue --replicas=3
kubectl scale deployment consultant-backend-green --replicas=0

# 3. Verify Blue is healthy
kubectl get pods -l version=blue -n consultant-backend
```

### Full rollback (< 30 minutes)

```bash
# 1. Restore old deployment without Infisical
kubectl apply -f kubernetes/deployment-old.yaml

# 2. Restore old secrets from backup
kubectl apply -f kubernetes/secrets-backup.yaml

# 3. Restart pods
kubectl rollout restart deployment/consultant-backend

# 4. Verify
kubectl rollout status deployment/consultant-backend
```

## Success Criteria

✅ **Technical:**

- [ ] All environments using Infisical (dev, staging, prod)
- [ ] Zero secrets in .env files or git
- [ ] Kubernetes Operator syncing secrets < 60s
- [ ] No production incidents related to secrets
- [ ] Health checks passing 99.9%+

✅ **Operational:**

- [ ] Team comfortable with new workflow
- [ ] Audit logs enabled and reviewed weekly
- [ ] Rotation policy documented and followed
- [ ] Backup/restore tested successfully
- [ ] CI/CD pipeline integrated

✅ **Security:**

- [ ] All production secrets rotated
- [ ] MFA enabled for all team members
- [ ] RBAC properly configured
- [ ] No hardcoded secrets in code
- [ ] Penetration test passed

## Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Service token expired | High | Low | Multiple tokens, monitoring, auto-renewal |
| Infisical API downtime | High | Very Low | Fallback to cached secrets, SLA 99.99% |
| Secrets not syncing | Medium | Low | Init containers, health checks, alerts |
| Team resistance | Medium | Medium | Training, documentation, support |
| Migration bugs | Low | Medium | Blue-green deployment, extensive testing |

## Contact & Support

- **Infisical Support:** <https://infisical.com/slack>
- **Documentation:** <https://infisical.com/docs>
- **Internal Contact:** DevOps team lead
- **Emergency:** On-call rotation

## Post-Migration Audit (1 month)

- [ ] Review all audit logs
- [ ] Check for security incidents
- [ ] Measure developer satisfaction
- [ ] Document cost savings (vs alternatives)
- [ ] Update security compliance docs
- [ ] Plan for future improvements (dynamic secrets, etc.)

---

**Status Tracking:** Update this checklist as you progress through migration.
**Last Updated:** 2026-01-13
