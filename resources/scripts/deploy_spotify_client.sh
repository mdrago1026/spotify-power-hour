## Deploy Spotify Client Script ##
PID=`ps aux | grep spotify-client | grep -v grep | awk '{print $2}'`

if [[ "" != "${PID}" ]]; then
  echo Spotify Client is running on PID: ${PID}
  echo Stopping Spotify Client..
  kill ${PID}
else
  echo Spotify Client is not currently running!
fi

base_dir='/home/mdrago/spotify_client/'
git_dir="$base_dir/spotify-power-hour"
final_jar_path="$git_dir/target/uberjar/spotify-client-*standalone.jar"
final_log_path="$base_dir/logs/spotify-client.out"
cd $git_dir
echo "Checking out branch: $1"
git fetch
git pull
git checkout $1
echo "CURRENT COMMIT: "
git rev-parse HEAD
#lein migratus migrate
rm -rf target
echo "Building jar.."
lein uberjar
echo "Jar built, starting jar at: $final_jar_path"
java -jar $final_jar_path '127.0.0.1' 1029 > $final_log_path 2>&1 &
echo "Start with PID: $!"
echo "Log dir: $final_log_path"
echo "done"
